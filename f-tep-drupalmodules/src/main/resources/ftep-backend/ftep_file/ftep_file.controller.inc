<?php 
/**
 * Custom controller for the lawmakers entity.
 */
class FtepFilesController extends EntityAPIController { }
class FtepFileUIController extends EntityDefaultUIController {
	/**
	 * Overrides hook_menu() defaults.
	 */
	public function hook_menu() {
		$items = parent::hook_menu ();
		$items [$this->path] ['description'] = 'Manage Files';
		return $items;
	}
}


