<?php 

/**
 * Implements hook_form().
 */
function ftep_databasket_form($form, &$form_state, $databasket = NULL) {
	$form = array();

	$form['name'] = array(
			'#title' => t('name'),
			'#type' => 'textfield',
			'#default_value' => isset($databasket->name) ? $databasket->name : '',
			'#description' => t('Fake User Name assigned to Datasource.'),
			'#required' => TRUE,
			'#maxlength' => 255,
	);

	$form['description'] = array(
			'#title' => t('Description'),
			'#type' => 'textfield',
			'#default_value' => isset($databasket->description) ? $databasket->description : '',
			'#required' => TRUE,
			'#maxlength' => 255,
	);
	field_attach_form('databasket', $databasket, $form, $form_state);
	
	$form['actions'] = array(
			'#type' => 'actions',
			'submit' => array(
					'#type' => 'submit',
					'#value' => isset($databasket->idb) ? t('Update Datasource') : t('Save Datasource'),
			),
			'delete_link' => array(
					'#markup' => isset($databasket->idb) ? l(t('Delete'), 'admin/content/databasket/manage/' . $databasket->idb . '/delete', array('attributes' => array('id' => array('databasket-delete-' . $databasket->idb), 'class' => array('button remove')), 'query' => array('destination' => 'admin/content/databaskets'))) : ''));
			return $form;
}
/**
 * Implements hook_form_validate().
 */
function ftep_databasket_form_validate($form, &$form_state) {
}


/**
 * Implements hook_form_submit().
 */
function ftep_databasket_form_submit($form, &$form_state) {
	$databasket = entity_ui_form_submit_build_entity($form, $form_state);
	$databasket->save();
	drupal_set_message(t('@name databasket has been saved.', array('@name' => $databasket->name)));
	// $form_state['redirect'] = 'admin/content/lawmakers';
	$form_state['redirect'] = 'databaskets/' . $databasket->idb;
}
