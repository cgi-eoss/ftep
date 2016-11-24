<?php 


/**
 * Implements hook_form().
 */
function ftep_datasource_form($form, &$form_state, $datasource = NULL) {
	$form = array();

	$form['name'] = array(
			'#title' => t('name'),
			'#type' => 'textfield',
			'#default_value' => isset($datasource->name) ? $datasource->name : '',
			'#description' => t('Fake User Name assigned to Datasource.'),
			'#required' => TRUE,
			'#maxlength' => 255,
	);

	$form['description'] = array(
			'#title' => t('Description'),
			'#type' => 'textfield',
			'#default_value' => isset($datasource->description) ? $datasource->description : '',
			'#required' => TRUE,
			'#maxlength' => 255,
	);
	$form['endpoint'] = array(
			'#title' => t('Endpoint'),
			'#type' => 'textfield',
			'#default_value' => isset($datasource->endpoint) ? $datasource->endpoint : '',
			'#required' => TRUE,
			'#maxlength' => 255,
	);
	$form['template'] = array(
			'#title' => t('Template'),
			'#type' => 'textarea',
			'#default_value' => isset($datasource->template) ? $datasource->template : '',
			'#required' => TRUE,
	);
	$form['parser'] = array(
			'#title' => t('Parser'),
			'#type' => 'textfield',
			'#default_value' => isset($datasource->parser) ? $datasource->parser : '',
			'#required' => TRUE,
			'#maxlength' => 255,
    );

    $form['enabled']['enabled'] = array(
        '#title' => t('Enabled'),
        '#type' => 'checkbox',
        '#options' => array( '1' => t("Yes"), '0' => t("No") ),
        '#default_value' => isset($datasource->enabled) ? $datasource->enabled : 0,
    );
	
	
	$form['policy'] = array(
			'#title' => t('Policy'),
			'#type' => 'textfield',
			'#default_value' => isset($datasource->description) ? $datasource->description : '',
			'#required' => TRUE,
			'#maxlength' => 255,
	);
	field_attach_form('datasource', $datasource, $form, $form_state);
	
	$form['actions'] = array(
			'#type' => 'actions',
			'submit' => array(
					'#type' => 'submit',
					'#value' => isset($datasource->id) ? t('Update Datasource') : t('Save Datasource'),
			),
			'delete_link' => array(
					'#markup' => isset($datasource->id) ? l(t('Delete'), 'admin/content/datasource/manage/' . $datasource->id . '/delete', array('attributes' => array('id' => array('datasource-delete-' . $datasource->id), 'class' => array('button remove')), 'query' => array('destination' => 'admin/content/datasources'))) : ''));
			return $form;
}
/**
 * Implements hook_form_validate().
 */
function ftep_datasource_form_validate($form, &$form_state) {
}


/**
 * Implements hook_form_submit().
 */
function ftep_datasource_form_submit($form, &$form_state) {
	$datasource = entity_ui_form_submit_build_entity($form, $form_state);
	$datasource->save();
	drupal_set_message(t('@name datasource has been saved.', array('@name' => $datasource->name)));
	// $form_state['redirect'] = 'admin/content/lawmakers';
	$form_state['redirect'] = 'datasources/' . $datasource->id;
}
