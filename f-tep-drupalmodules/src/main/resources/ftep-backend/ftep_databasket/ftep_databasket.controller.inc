<?php 
/**
 * Custom controller for the lawmakers entity.
 */
class FtepDatabasketsController extends EntityAPIController { }
class FtepDatabasketUIController extends EntityDefaultUIController {
	/**
	 * Overrides hook_menu() defaults.
	 */
	public function hook_menu() {
		$items = parent::hook_menu ();
		$items [$this->path] ['description'] = 'Manage Databaskets';
		return $items;
	}
}


