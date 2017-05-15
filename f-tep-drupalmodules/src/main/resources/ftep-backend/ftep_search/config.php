<?php

use Gelf\Publisher;
use Gelf\Transport\HttpTransport;
use Monolog\Formatter\LineFormatter;
use Monolog\Handler\GelfHandler;
use Monolog\Handler\RotatingFileHandler;
use Monolog\Logger;
use Monolog\Processor\MemoryUsageProcessor;
use Monolog\Processor\WebProcessor;
use Psr\Log\LoggerInterface;
use phpFastCache\CacheManager;
use Symfony\Component\Yaml\Yaml;

require_once __DIR__ . '/ftep_resource_search.php';
require_once __DIR__ . '/ftep_resource_downloadmanager.php';
require_once __DIR__ . '/ftep_resource_identitymanager.php';
require_once __DIR__ . '/ftep_resource_job.php';
require_once __DIR__ . '/ftep_resource_datasource.php';
require_once __DIR__ . '/ftep_resource_databasket.php';
require_once __DIR__ . '/ftep_resource_service.php';
require_once __DIR__ . '/ftep_resource_group.php';
require_once __DIR__ . '/ftep_resource_project.php';
require_once __DIR__ . '/ftep_resource_user.php';

require_once __DIR__ . '/FileDatasourceProvider.php';
require_once __DIR__ . '/DrupalDatasourceProvider.php';

$LOG_LEVELS = array('DEBUG', 'INFO', 'NOTICE', 'WARNING', 'ERROR', 'CRITICAL', 'ALERT', 'EMERGENCY');

return [
    Psr\Log\LoggerInterface::class => DI\factory(function (\DI\Container $c) use ($LOG_LEVELS) {
        $level = $c->get('log.level');

        if (!in_array($level, $LOG_LEVELS, true)) {
            die('Invalid value for \'log.level\' : \'' . $level . '\'. Must be one of : ' . implode(',', $LOG_LEVELS));
        }

        $logLevel = constant(Logger::class . '::' . $level);

        $fileHandler = new RotatingFileHandler($c->get('log.filename'), 0, $logLevel);
        $transport = HttpTransport::fromUrl($c->get('log.url'));
        //$transport = new TcpTransport($c->get('log.url'),12201);

        $gelfHandler = new GelfHandler(new Publisher($transport), $logLevel, 0);

        $logger = new Logger('ftep');
        $logger->pushProcessor(new WebProcessor(null,[
            'HTTP_USER_AGENT'=>'HTTP_USER_AGENT',
            'HTTP_VIA'=>'HTTP_VIA',
            'HTTP_X_FORWARDED_FOR'=>'HTTP_X_FORWARDED_FOR',
            'HTTP_X_FORWARDED_HOST'=>'HTTP_X_FORWARDED_HOST',
            'HTTP_X_FORWARDED_SERVER'=>'HTTP_X_FORWARDED_SERVER'
        ]));
        $logger->pushProcessor(new MemoryUsageProcessor);

        $logger->pushProcessor(
            function ($record) {
                global $user;
                $record['extra']['uid'] = $user ? $user->uid : 'n.a.';
                $record['extra']['name'] = $user ? $user->name : 'n.a.';
                return $record;
            }
        );

        $fileHandler->setFormatter(new LineFormatter());

        $logger->pushHandler($fileHandler);
        // $logger->pushHandler($gelfHandler);
        return $logger;
    }),

    'CredentialResolverService' => DI\factory(function (\DI\Container $c)  {
        $logger = $c->get(Psr\Log\LoggerInterface::class);
        $logger->debug(__METHOD__ . ' - ' . __LINE__ . ' Loading credentials ');
        $result = null;
        $config = $c->get('configFile');
        try{
            $result =  Yaml::parse(file_get_contents($config['credentials']));
        } catch (ParseException $e) {
            $logger->err(__METHOD__ . ' - ' . __LINE__ . ' Unable to parse the configuration file '
                .$config['credentials'].' : '. $e->getMessage());
        }
        return $result;
    }),

    'CacheService' =>  DI\factory(function (\DI\Container $c) use ($LOG_LEVELS) {
        CacheManager::setDefaultConfig(array(
            "path" => '/tmp/ql_cache/', // or in windows "C:/tmp/"
            'defaultTtl' => 86400, // expiration time in seconds
        ));

        $cache = CacheManager::getInstance('files');

        return $cache;
    }),


    FtepResourceSearch::class => DI\object()->constructor(),
    FtepResourceJob::class => DI\object()->constructor(),
    FtepResourceIdentityManager::class => DI\object()->constructor(),
    FtepResourceDataSource::class => DI\object()->constructor(),
    FtepResourceDataBasket::class => DI\object()->constructor(),
    FtepResourceService::class => DI\object()->constructor(),
    FtepResourceGroup::class => DI\object()->constructor(),
    FtepResourceProject::class => DI\object()->constructor(),

    'DatasourceProviderService' => DI\object(FileDatasourceProvider::class)->constructor(),
    //'DatasourceProviderService' =>  DI\object( DrupalDatasourceProvider::class )->constructor(),

    'log.url' => 'http://ftep-monitor.eoss-cloud.it/gelf',
    'log.filename' => '/var/log/ftep.log',
    'log.level' => 'DEBUG',

    'debug' => false,

    // 'configFile' => __DIR__ . '/config.yml',

    'configFile' => DI\factory(function (\DI\Container $c)  {

        $logger = $c->get(Psr\Log\LoggerInterface::class);
        $conf = __DIR__ . '/config.yml';
        $value = null;
        try{
            $value = Yaml::parse(file_get_contents($conf));
        } catch (ParseException $e) {
            $logger->err(__METHOD__ . ' - ' . __LINE__ . ' Unable to parse the configuration file '
                .$conf.' : '. $e->getMessage());
        }
        return $value;
    }),

    // 'drupal.root' => __DIR__ . '/drupal-7.52/',
    // 'module.root' => __DIR__ . '/drupal-7.52//sites/all/modules/',
    'api.version'   => '1.0',
    'drupal.root' => '/var/www/html/drupal/forestry-tep.eo.esa.int/',
    'module.root' => '/var/www/html/drupal/forestry-tep.eo.esa.int/sites/all/modules/',
];
