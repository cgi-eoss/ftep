<?php
class FtepGroupsController extends EntityAPIController { } 

class FtepGroupUIController extends EntityDefaultUIController {
    	/**
    	 * Overrides hook_menu() defaults.
    	 */
    	public function hook_menu() {
    		$items = parent::hook_menu ();
    		$items [$this->path] ['description'] = 'Manage Groups';
    		return $items;
    	}
    }
    
