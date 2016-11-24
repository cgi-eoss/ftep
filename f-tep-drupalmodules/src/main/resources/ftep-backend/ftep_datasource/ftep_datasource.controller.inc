<?php 
/**
 * Custom controller for the lawmakers entity.
 */
class FtepDatasourcesController extends EntityAPIController {

// 	/**
// 	 * Override the save method.
// 	 */
// 	public function save($entity, DatabaseTransaction $transaction = NULL) {
// 		if (isset($entity->is_new)) {
// 			$entity->created = REQUEST_TIME;
// 		}

// 		$entity->changed = REQUEST_TIME;
// 		return parent::save($entity, $transaction);
// 	}
}
class FtepDatasourceUIController extends EntityDefaultUIController {
	/**
	 * Overrides hook_menu() defaults.
	 */
	public function hook_menu() {
		$items = parent::hook_menu ();
		$items [$this->path] ['description'] = 'Manage Datasources';
		return $items;
	}
}


