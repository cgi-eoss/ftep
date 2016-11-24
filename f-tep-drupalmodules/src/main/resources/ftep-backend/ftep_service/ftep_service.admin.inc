<?php 


/**
 * Implements hook_form().
 */
function ftep_service_form($form, &$form_state, $service = NULL) {
    $form = array();

    $form['name'] = array(
            '#title' => t('name'),
            '#type' => 'textfield',
            '#default_value' => isset($service->name) ? $service->name : '',
            '#description' => t('Name assigned to Service.'),
            '#required' => TRUE,
            '#maxlength' => 255,
    );

    $form['description'] = array(
            '#title' => t('Description'),
            '#type' => 'textfield',
            '#default_value' => isset($service->description) ? $service->description : '',
            '#required' => TRUE,
            '#maxlength' => 255,
    );
    
    $form['kind'] = array(
            '#title' => t('Service Type'),
            '#type' => 'textfield',
            '#default_value' => isset($service->kind) ? $service->kind : '',
            '#required' => TRUE,
            '#maxlength' => 255,
    );
    
    $form['mode'] = array(
            '#title' => t('Service Mode'),
            '#type' => 'textfield',
            '#default_value' => isset($service->mode) ? $service->mode : '',
            '#required' => TRUE,
            '#maxlength' => 255,
    );
    $form['rating'] = array(
            '#title' => t('Service Rating'),
            '#type' => 'textfield',
            '#default_value' => isset($service->rating) ? $service->rating : '',
            '#required' => TRUE,
            '#maxlength' => 255,
    );
    $form['access'] = array(
            '#title' => t('Access Level '),
            '#type' => 'textfield',
            '#default_value' => isset($service->access_level) ? $service->access_level : '',
            '#required' => TRUE,
            '#maxlength' => 255,
    );
    $form['cpu'] = array(
            '#title' => t('CPU'),
            '#type' => 'textfield',
            '#default_value' => isset($service->access_level) ? $service->access_level : '',
            '#required' => TRUE,
            '#maxlength' => 255,
    );
    $form['ram'] = array(
            '#title' => t('Ram'),
            '#type' => 'textfield',
            '#default_value' => isset($service->access_level) ? $service->access_level : '',
            '#required' => TRUE,
            '#maxlength' => 255,
    );
    $form['cost'] = array(
            '#title' => t('Cost'),
            '#type' => 'textfield',
            '#default_value' => isset($service->access_level) ? $service->access_level : '',
            '#required' => TRUE,
            '#maxlength' => 255,
    );
    $form['license'] = array(
            '#title' => t('License'),
            '#type' => 'textfield',
            '#default_value' => isset($service->access_level) ? $service->access_level : '',
            '#required' => TRUE,
            '#maxlength' => 255,
    );
    $form['status'] = array(
            '#title' => t('Status'),
            '#type' => 'textfield',
            '#default_value' => isset($service->access_level) ? $service->access_level : '',
            '#required' => TRUE,
            '#maxlength' => 255,
    );
	global $user;
	$form['user_id'] = array(
			'#title' => t('userid'),
			'#type' => 'textfield',
			'#default_value' => isset($user->uid) ? $user->uid : '',
			'#required' => TRUE,
			'#readonly' => "readonly",
			'#maxlength' => 255,
	);
	

    field_attach_form('service', $service, $form, $form_state);
    
    $form['actions'] = array(
            '#type' => 'actions',
            'submit' => array(
                    '#type' => 'submit',
                    '#value' => isset($service->id) ? t('Update Service') : t('Save Service'),
            ),
            'delete_link' => array(
                    '#markup' => isset($service->id) ? l(t('Delete'), 'admin/content/service/manage/' . $service->id . '/delete', array('attributes' => array('id' => array('service-delete-' . $service->id), 'class' => array('button remove')), 'query' => array('destination' => 'admin/content/services'))) : ''));
            return $form;
}
/**
 * Implements hook_form_validate().
 */
function ftep_service_form_validate($form, &$form_state) {
}


/**
 * Implements hook_form_submit().
 */
function ftep_service_form_submit($form, &$form_state) {
    $service = entity_ui_form_submit_build_entity($form, $form_state);
    $service->save();
    drupal_set_message(t('@name service has been saved.', array('@name' => $service->name)));
    // $form_state['redirect'] = 'admin/content/lawmakers';
    $form_state['redirect'] = 'services/' . $service->sid;
}

