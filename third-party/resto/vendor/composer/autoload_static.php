<?php

// autoload_static.php @generated by Composer

namespace Composer\Autoload;

class ComposerStaticInit60be11f9195a496f2742f075e3f5f065
{
    public static $prefixesPsr0 = array (
        'U' => 
        array (
            'Utilities\\' => 
            array (
                0 => __DIR__ . '/..' . '/skyscanner/jsonpath/src/Skyscanner',
            ),
        ),
        'J' => 
        array (
            'JsonPath\\' => 
            array (
                0 => __DIR__ . '/..' . '/skyscanner/jsonpath/src/Skyscanner',
            ),
        ),
    );

    public static function getInitializer(ClassLoader $loader)
    {
        return \Closure::bind(function () use ($loader) {
            $loader->prefixesPsr0 = ComposerStaticInit60be11f9195a496f2742f075e3f5f065::$prefixesPsr0;

        }, null, ClassLoader::class);
    }
}
