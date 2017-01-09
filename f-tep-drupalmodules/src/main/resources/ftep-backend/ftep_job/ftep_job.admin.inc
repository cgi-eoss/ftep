<?php

/**
 * Page to select ftep4 Type to add new ftep4.
 */
function ftep_job_admin_add_page() {
  $items = array();
  foreach (ftep_job_types() as $ftep4_type_key => $ftep4_type) {
    $items[] = l(entity_label('ftep_job_type', $ftep4_type), 'ftep4/add/' . $ftep4_type_key);
  }
  return array('list' => array('#theme' => 'item_list', '#items' => $items, '#title' => t('Select type of ftep4 to create.')));
}

/**
 * Add new ftep4 page callback.
 */
function ftep_job_add($type) {
  $ftep4_type = ftep_job_types($type);

  $ftep4 = entity_create('ftep_job', array('type' => $type));
  drupal_set_title(t('Create @name', array('@name' => entity_label('ftep4_type', $ftep4_type))));

  $output = drupal_get_form('ftep_job_form', $ftep4);

  return $output;
}

/**
 * ftep4 Form.
 */
function ftep_job_form($form, &$form_state, $job=NULL) {
	global $user;   
	$form = array();
	$form['jid'] = array(
			'#title' => t('JobID'),
			'#type' => 'textfield',
			'#default_value' => isset($job->jid) ? $job->jid : '',
			'#required' => TRUE,
			'#maxlength' => 255,
	);
	
	$form['inputs'] = array(
			'#title' => t('Input String (Json)'),
			'#type' => 'textfield',
			'#default_value' => isset($job->inputs) ? $job->inputs : '',
			'#required' => TRUE,
			'#maxlength' => 255,
	);
	
	$form['outputs'] = array(
			'#title' => t('Output String (Json)'),
			'#type' => 'textfield',
			'#default_value' => isset($job->outputs) ? $job->outputs : '',
			'#required' => TRUE,
			'#maxlength' => 255,
	);
	
	$form['guiendpoint'] = array(
			'#title' => t('Gui Endpoint)'),
			'#type' => 'textfield',
			'#default_value' => isset($job->guiendpoint) ? $job->guiendpoint : '',
			'#required' => TRUE,
			'#maxlength' => 255,
	);
	
	
	$form['uid'] = array(
			'#title' => t('userid'),
			'#type' => 'textfield',
			'#default_value' => isset($user->uid) ? $user->uid : '',
			'#required' => TRUE,
			'#readonly' => "readonly",
			'#maxlength' => 255,
	);
	
	$form['actions'] = array(
			'#type' => 'actions',
			'submit' => array(
					'#type' => 'submit',
					'#value' => isset($job->jid) ? t('Update Job') : t('Save Job'),
			),
			'delete_link' => array(
					'#markup' => isset($job->jid) ? l(t('Delete'), 'admin/content/job/manage/' . $job->jid . '/delete', array('attributes' => array('jid' => array('jobs-delete-' . $job->jid), 'class' => array('button remove')), 'query' => array('destination' => 'admin/content/jobs'))) : ''));
			return $form;
			
	field_attach_form('job', $job, $form, $form_state);
	
	$form['actions'] = array(
			'#type' => 'actions',
			'submit' => array(
					'#type' => 'submit',
					'#value' => isset($job->jid) ? t('Update Job') : t('Save Job'),
			),
			'delete_link' => array(
					'#markup' => isset($job->jid) ? l(t('Delete'), 'admin/content/job/manage/' . $job->jid . '/delete', array('attributes' => array('jid' => array('job-delete-' . $job->jid), 'class' => array('button remove')), 'query' => array('destination' => 'admin/content/jobs'))) : ''));
	return $form;
}

function ftep_job_form_validate($form, &$form_state) {
    $node_id = $form_state["values"]["nid"];
    if(node_load($node_id) === FALSE){
        form_set_error("$nid", t('The node id you have entered is not a node'));
    }
}
/**
 * Implements hook_form_submit().
 */
function ftep_job_form_submit($form, &$form_state) {
	$job = entity_ui_form_submit_build_entity($form, $form_state);
	$job->save();
	$job_uri = entity_uri('ftep_job', $job);
	drupal_set_message(t('job %title saved.', array('%title' => entity_label('ftep_job', $job))));
	$form_state['redirect'] = $job_uri['path'];
}

/**
 * Delete confirmation form.
 */
function ftep_job_delete_form($form, &$form_state, $ftep4) {
  $form_state['ftep4'] = $ftep4;
  // Always provide entity id in the same form key as in the entity edit form.
  $form['ftep4_type_id'] = array('#type' => 'value', '#value' => entity_id('ftep_job' ,$ftep4));
  $ftep4_uri = entity_uri('ftep_job', $ftep4);
  return confirm_form($form,
    t('Are you sure you want to delete ftep4 %title?', array('%title' => entity_label('ftep_job', $ftep4))),
    $ftep4_uri['path'],
    t('This action cannot be undone.'),
    t('Delete'),
    t('Cancel')
  );
}

/**
 * Delete form submit handler.
 */
function ftep_job_delete_form_submit($form, &$form_state) {
  $ftep4 = $form_state['ftep4'];
  ftep_job_delete($ftep4);

  drupal_set_message(t('ftep4 %title deleted.', array('%title' => entity_label('ftep_job', $ftep4))));

  $form_state['redirect'] = '<front>';
}

