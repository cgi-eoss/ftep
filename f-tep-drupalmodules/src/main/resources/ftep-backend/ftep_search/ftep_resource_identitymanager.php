<?php
require_once __DIR__ . '/ftep_resource.php';
require __DIR__ . '/vendor/autoload.php';
require __DIR__ . '/serializers/LoginSerializer.php';

use Tobscure\JsonApi\Document;
use Tobscure\JsonApi\EmptyCollection;
use Tobscure\JsonApi\EmptyResource;
use Tobscure\JsonApi\ErrorHandler;
use Tobscure\JsonApi\Exception\Handler\FallbackExceptionHandler;
use Tobscure\JsonApi\Resource;

class FtepResourceIdentityManager extends FtepResource
{
    public function handleError($e, $error, $route)
    {
        $errors = new ErrorHandler();
        $errors->registerHandler(new FallbackExceptionHandler(true));
        $response = $errors->handle($e);
        $document = new Document;
        $document->setErrors($response->getErrors());
        http_response_code($response->getStatus());
        return $document;
    }

    public function authorize($route, $bootstrap, $config)
    {
        //require_once MODULE_ROOT . '/shib_auth/shib_auth.module'; //REMOVEME

        foreach($_COOKIE as $k=>$v){
            $_COOKIE[$k] = explode(",", $v)[0] ;
        }
        drupal_bootstrap(DRUPAL_BOOTSTRAP_DATABASE);
        // Prevent Devel from hi-jacking our output in any case.
        $GLOBALS['devel_shutdown'] = FALSE;
        // Deactivate Drupal Error and Exception handling.
        restore_error_handler();
        restore_exception_handler();
        // Activate Endpoint error handler.
        set_error_handler('endpoint_error_handler');
        drupal_bootstrap(DRUPAL_BOOTSTRAP_FULL);

        $bootstrap = isset($route['bootstrap']) ? $route['bootstrap'] : DRUPAL_BOOTSTRAP_DATABASE;
        if ($bootstrap < DRUPAL_BOOTSTRAP_FULL) {
            fix_gpc_magic();
        }

        if ($bootstrap > DRUPAL_BOOTSTRAP_DATABASE) {
            drupal_bootstrap($bootstrap);
        }

        if (($bootstrap < DRUPAL_BOOTSTRAP_SESSION)) {
            require_once DRUPAL_ROOT . '/' . variable_get('session_inc', 'includes/session.inc');
            drupal_session_initialize();
        }
        $x = $this->shib_auth_init2();

        if (!$GLOBALS['user']->uid && empty($route['anonymous'])) {
            throw new Exception('User should be authorized!', 401);
        }
    }

    /**
     * Create a new user based on informations from the Shibboleth handler if it's necessary or log in.
     *
     * If already authenticated - do nothing
     * If Shibboleth doesn't provide User information - error message
     * Else if user exists, and mail override (shib_auth_req_shib_only) enabled, override existing user info
     * If not exists, and Shibboleth provides mail address, create an account for this user
     * If there's no mail attribute, ask for the mail address on a generated form if mail override (shib_auth_req_shib_only) is disabled
     * In this case, the account will be created with this e-mail address.
     */
    function shib_auth_init2()
    {
        global $user;

        //add theme css
        drupal_add_css(drupal_get_path('module', 'shib_auth') . '/shib_auth.css');

        // Make sure that the user module is already loaded.
        drupal_load('module', 'user');

        $consent_accepted = FALSE;

        /* We want to return as early as possible if we have nothing to do.
        But for checking the session, we need the username first (if it's set) */
        $uname = shib_auth_getenv(shib_auth_config('username_variable')); # Might be NULL

        // Storing whether the user was already logged in or not
        $alreadyloggedin = (user_is_anonymous()) ? False : True;

        /* CHECKING THE SESSION
         Here shib_auth_session_check() will destroy the session if
         * the shib session is expired and auto_destroy_session is enabled
         * the username has changed unexpectedly
         Either this happens or we do not have a shib session, we don't have anything to do
         but send out some debug and exit.
         */
        if (!shib_auth_session_check($uname) || !shib_auth_session_valid()) {
            shib_auth_debug();
            return;
        }

        /* Time to retrevie the mail and begin some work */
        $umail = shib_auth_getenv(shib_auth_config('email_variable')) ? shib_auth_getenv(shib_auth_config('email_variable')) : '';
        $umail_single = preg_replace('/;.*/', '', $umail); // get the first one if there're many

        //************ ROLE ASSIGMENT  **************
        shib_auth_role_assignment();

        //**************** DEBUG ********************
        shib_auth_debug();

        // Do nothing if the user is logged in and we're not doing account linking
        if ($user->uid && empty($_SESSION['shib_auth_account_linking']))
            return;

        // Do virtually nothing when we need to display the custom data form
        if (isset($_SESSION['shib_auth_custom_form']) && $_SESSION['shib_auth_custom_form']) {
            unset($_SESSION['shib_auth_custom_form']); // Display it only once
            return;
        }
        /********* Start the login/registering process **********/
        //check identifier if it exists, and not too long
        if (!shib_auth_check_identifier($uname)) {
            shib_auth_error('Shibboleth authentication process can\'t continue');
            return;
        }
        //check if the old user exists in the shibboleth authmap
        $existing_authmap = shib_auth_load_from_authmap($uname);
//           die("<Pre>".var_export(array( $existing_authmap, $uname) ,true));

        //Check whether CONSENT VERSION is CHANGED, if so, users have to accept it again
        if (isset($_POST['form_id']) && $_POST['form_id'] == 'shib_auth_custom_data' &&
            !empty($_POST['accept'])
        ) {
            $consent_accepted = filter_xss($_POST['accept']);
        }

        //*********** LOGIN EXISTING USER ***************
        //The user exists in the authmap, and the consent version check is switched off, or she/he had accepted the newest consent version
        //Then let the user log in
        if ($existing_authmap
            && (!shib_auth_config('terms_accept') || ($existing_authmap['consentver'] == shib_auth_config('terms_ver')))
        ) {
            if (empty($_SESSION['shib_auth_account_linking'])) {
                shib_login_authmap($uname, $umail_single, $existing_authmap['uid'], $alreadyloggedin);
            } else {
                shib_auth_terminate_session('This ID has already been registered, please log in again');
            }
        }
        //The user exists in the authmap, and she had just accepted the new consent version
        //Write the new version number into the her authmap row, and log her in
        elseif ($existing_authmap && $consent_accepted) {
            shib_auth_consent_update($uname, $umail_single, $existing_authmap['uid']);
        }
        //********* END OF LOGIN CASE *************

        //********* REGISTER NEW USER *************
        //The user doesn't exists in the database, starting registering process
        else {
            //If it is account linking and the terms are accepted or forcing an existing user to accept termsandconditions
            //If we have an e-mail address from the shib server, and there isn't any user with this address, create an account with these infos
            if (!empty($_SESSION['shib_auth_account_linking'])
                || $umail_single
                && !shib_auth_config('enable_custom_mail')
                && !shib_auth_config('define_username')
                && !shib_auth_config('terms_accept')
            ) {
                shib_auth_save_authmap($uname, $uname, $umail_single);
            }

            //********* CUSTOM OPTION ENABLED **********
            //Lock user into the customization / consent form, unless it is the terms and contitions page itself
            elseif (($_GET['q']) == shib_auth_config('terms_url')) {
                //Don't display custom form, let the terms and conditions be displayed
            } //if one of the customizing options enabled, ask for these values, then register her
            elseif (shib_auth_custom_form($umail_single, $uname)) {
                //We display custom forms on every page, if the user isn't registered yet
            } //If there is no custom mail option enabled, and we didn't received email address from server, output an error
            else {
                shib_auth_error('E-mail address is missing. Please contact your site administrator!');
            }
        }
        //****** ASSIGN ROLES AFTER REGISTER *******
        shib_auth_role_assignment();

        //********* END OF REGISTERING *************
        if (isset($_SESSION['shib_auth_account_linking']) && $_SESSION['shib_auth_account_linking']) {
            unset($_SESSION['shib_auth_account_linking']);
            drupal_set_message(t('End of account linking session'));
        }
    }// function shib_auth_init()

    public function login()
    {
        global $user;
        // $data = endpoint_request_data();
        $data = $this->getSection('login');
        $username = $data->user;
        $password = $data->password;

        $user = $this->getUser();
        if ($user->uid) {
            // HTTP error 406
            http_response_code("204");
            //die("Already logged in as ".$user->name);
        }
        drupal_load('module', 'user');
        if (user_is_blocked($data->user)) {
            // HTTP error 403
            die("Username " . $data->user . " is not active or blocked");
        }
        drupal_bootstrap(DRUPAL_BOOTSTRAP_FULL);
        // Emulate drupal native flood control: check for flood condition.
        $flood_state = array();
        if (variable_get('services_flood_control_enabled', TRUE)) {
            $flood_state = $this->_user_resource_flood_control_precheck($username);
        }

        // Only authenticate if a flood condition was not detected.
        if (empty($flood_state['flood_control_triggered'])) {
            $uid = user_authenticate($username, $password);
        } else {
            $uid = FALSE;
        }

        // Emulate drupal native flood control: register flood event, and throw error
        // if a flood condition was previously detected
        if (variable_get('services_flood_control_enabled', TRUE)) {
            $flood_state['uid'] = $uid;
            $this->_user_resource_flood_control_postcheck($flood_state);
        }

        if ($uid) {
            $user = user_load($uid);
            if ($user->uid) {
                user_login_finalize();

                $return = new stdClass();
                $return->sessid = session_id();
                $return->session_name = session_name();
                $return->token = drupal_get_token('services');
                $resource = new Resource((object)$return, new LoginSerializer);
                return $this->writeJsonApiResponse($resource, null, 0, 201);

                return $return;
            }
        }

        watchdog('user', 'Invalid login attempt for %username.', array('%username' => $username));
        throw new Exception(t('Wrong username or password'));
    }

    /**
     * Emulate native Drupal flood control, phase 1.
     *
     * This function checks for a flood condition, and determines the identifier
     * for user based flood checks. This is done prior to user authentication.
     *
     * @param string $username
     *   The name of the user who is attempting to log in.
     * @return array
     *   An array containing zero or more of the following keys:
     *   - flood_control_triggered: either 'user' or 'ip' if a flood condition
     *     was detected.
     *   - flood_control_user_identifier: the identifier to use to register
     *     user-based flood events.
     *
     * @see _user_resource_flood_control_postcheck().
     * @see user_login_authenticate_validate().
     */
    function _user_resource_flood_control_precheck($username)
    {
        $flood_state = array();
        // Do not allow any login from the current user's IP if the limit has been
        // reached. Default is 50 failed attempts allowed in one hour. This is
        // independent of the per-user limit to catch attempts from one IP to log
        // in to many different user accounts.  We have a reasonably high limit
        // since there may be only one apparent IP for all users at an institution.
        if (!flood_is_allowed('failed_login_attempt_ip', variable_get('user_failed_login_ip_limit', 50), variable_get('user_failed_login_ip_window', 3600))) {
            $flood_state['flood_control_triggered'] = 'ip';
        } else {
            $account = db_query("SELECT * FROM {users} WHERE name = :name AND status = 1", array(':name' => $username))->fetchObject();
            if ($account) {
                if (variable_get('user_failed_login_identifier_uid_only', FALSE)) {
                    // Register flood events based on the uid only, so they apply for any
                    // IP address. This is the most secure option.
                    $identifier = $account->uid;
                } else {
                    // The default identifier is a combination of uid and IP address. This
                    // is less secure but more resistant to denial-of-service attacks that
                    // could lock out all users with public user names.
                    $identifier = $account->uid . '-' . ip_address();
                }
                $flood_state['flood_control_user_identifier'] = $identifier;

                // Don't allow login if the limit for this user has been reached.
                // Default is to allow 5 failed attempts every 6 hours.
                if (!flood_is_allowed('failed_login_attempt_user', variable_get('user_failed_login_user_limit', 5), variable_get('user_failed_login_user_window', 21600), $identifier)) {
                    $flood_state['flood_control_triggered'] = 'user';
                }
            }
        }
        return $flood_state;
    }

    function _user_resource_flood_control_postcheck($flood_state)
    {
        if (empty($flood_state['uid'])) {
            // Always register an IP-based failed login event.
            flood_register_event('failed_login_attempt_ip', variable_get('user_failed_login_ip_window', 3600));
            // Register a per-user failed login event.
            if (isset($flood_state['flood_control_user_identifier'])) {
                flood_register_event('failed_login_attempt_user', variable_get('user_failed_login_user_window', 21600), $flood_state['flood_control_user_identifier']);
            }
            if (isset($flood_state['flood_control_triggered'])) {
                if ($flood_state['flood_control_triggered'] == 'user') {
                    services_error(t('Account is temporarily blocked.'), 406);
                } else {
                    // We did not find a uid, so the limit is IP-based.
                    services_error(t('This IP address is temporarily blocked.'), 406);
                }
            }
        } elseif (isset($flood_state['flood_control_user_identifier'])) {
            // Clear past failures for this user so as not to block a user who might
            // log in and out more than once in an hour.
            flood_clear_event('failed_login_attempt_user', $flood_state['flood_control_user_identifier']);
        }
    }
}
    


