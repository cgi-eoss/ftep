<?php
class FtepservicesController extends EntityAPIController { } 

class FtepServiceUIController extends EntityDefaultUIController {
    	/**
    	 * Overrides hook_menu() defaults.
    	 */
    	public function hook_menu() {
    		$items = parent::hook_menu ();
    		$items [$this->path] ['description'] = 'Manage Services';
    		return $items;
    	}
    }
    
