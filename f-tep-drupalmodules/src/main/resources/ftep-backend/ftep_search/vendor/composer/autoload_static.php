<?php

// autoload_static.php @generated by Composer

namespace Composer\Autoload;

class ComposerStaticInita54e3e03dcc2acb47c5a7a2340dc9a60
{
    public static $prefixLengthsPsr4 = array (
        'T' => 
        array (
            'Tobscure\\JsonApi\\' => 17,
        ),
        'P' => 
        array (
            'Psr\\Log\\' => 8,
        ),
        'M' => 
        array (
            'Monolog\\' => 8,
        ),
        'C' => 
        array (
            'Curl\\' => 5,
        ),
    );

    public static $prefixDirsPsr4 = array (
        'Tobscure\\JsonApi\\' => 
        array (
            0 => __DIR__ . '/..' . '/tobscure/json-api/src',
        ),
        'Psr\\Log\\' => 
        array (
            0 => __DIR__ . '/..' . '/psr/log/Psr/Log',
        ),
        'Monolog\\' => 
        array (
            0 => __DIR__ . '/..' . '/monolog/monolog/src/Monolog',
        ),
        'Curl\\' => 
        array (
            0 => __DIR__ . '/..' . '/php-curl-class/php-curl-class/src/Curl',
        ),
    );

    public static $classMap = array (
        'geoPHP' => __DIR__ . '/..' . '/phayes/geophp/geoPHP.inc',
    );

    public static function getInitializer(ClassLoader $loader)
    {
        return \Closure::bind(function () use ($loader) {
            $loader->prefixLengthsPsr4 = ComposerStaticInita54e3e03dcc2acb47c5a7a2340dc9a60::$prefixLengthsPsr4;
            $loader->prefixDirsPsr4 = ComposerStaticInita54e3e03dcc2acb47c5a7a2340dc9a60::$prefixDirsPsr4;
            $loader->classMap = ComposerStaticInita54e3e03dcc2acb47c5a7a2340dc9a60::$classMap;

        }, null, ClassLoader::class);
    }
}
