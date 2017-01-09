<?php 

/**
 * Implements hook_form().
 */
function ftep_file_form($form, &$form_state, $file = NULL) {
	$form = array();

	$form['name'] = array(
			'#title' => t('name'),
			'#type' => 'textfield',
			'#default_value' => isset($file->name) ? $file->name : '',
			'#description' => t('File Name'),
			'#required' => TRUE,
			'#maxlength' => 255,
	);

	$form['description'] = array(
			'#title' => t('Description'),
			'#type' => 'textfield',
			'#default_value' => isset($file->description) ? $file->description : '',
			'#required' => TRUE,
			'#maxlength' => 255,
	);
	field_attach_form('file', $file, $form, $form_state);
	
	$form['actions'] = array(
			'#type' => 'actions',
			'submit' => array(
					'#type' => 'submit',
					'#value' => isset($file->idb) ? t('Update file') : t('Save file'),
			),
			'delete_link' => array(
					'#markup' => isset($file->idb) ? l(t('Delete'), 'admin/content/file/manage/' . $file->idb . '/delete', array('attributes' => array('id' => array('file-delete-' . $file->idb), 'class' => array('button remove')), 'query' => array('destination' => 'admin/content/files'))) : ''));
			return $form;
}
/**
 * Implements hook_form_validate().
 */
function ftep_file_form_validate($form, &$form_state) {
}


/**
 * Implements hook_form_submit().
 */
function ftep_file_form_submit($form, &$form_state) {
	$file = entity_ui_form_submit_build_entity($form, $form_state);
	$file->save();
	drupal_set_message(t('@name file has been saved.', array('@name' => $file->name)));
	// $form_state['redirect'] = 'admin/content/lawmakers';
	$form_state['redirect'] = 'files/' . $file->idb;
}
