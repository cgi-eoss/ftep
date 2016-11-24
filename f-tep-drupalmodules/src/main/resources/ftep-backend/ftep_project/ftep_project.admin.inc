<?php


/**
 * ftep4 Form.
 */
function ftep_project_form($form, &$form_state, $project=NULL) {
    global $user;   
    $form = array();
    $form['name'] = array(
        '#title' => t('Name'),
        '#type' => 'textfield',
        '#default_value' => isset($project->name) ? $project->name : '',
        '#required' => TRUE,
        '#maxlength' => 255,
    );

    $form['description'] = array(
        '#title' => t('Project Description'),
        '#type' => 'textfield',
        '#default_value' => isset($project->description) ? $project->description : '',
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
            '#value' => isset($project->id) ? t('Update Project') : t('Save Project'),
        ),
        'delete_link' => array(
            '#markup' => isset($project->id) ? l(t('Delete'), 'admin/content/project/manage/' . $project->jid . '/delete', array('attributes' => array('jid' => array('projects-delete-' . $project->jid), 'class' => array('button remove')), 'query' => array('destination' => 'admin/content/projects'))) : ''));
    return $form;

    field_attach_form('project', $project, $form, $form_state);

    $form['actions'] = array(
        '#type' => 'actions',
        'submit' => array(
            '#type' => 'submit',
            '#value' => isset($project->jid) ? t('Update Project') : t('Save Project'),
        ),
        'delete_link' => array(
            '#markup' => isset($project->id) ? l(t('Delete'), 'admin/content/project/manage/' . $project->jid . '/delete', array('attributes' => array('jid' => array('project-delete-' . $project->jid), 'class' => array('button remove')), 'query' => array('destination' => 'admin/content/projects'))) : ''));
    return $form;
}

function ftep_project_form_validate($form, &$form_state) {
    $node_id = $form_state["values"]["nid"];
    if(node_load($node_id) === FALSE){
        form_set_error("$nid", t('The node id you have entered is not a node'));
    }
}
/**
 * Implements hook_form_submit().
 */
function ftep_project_form_submit($form, &$form_state) {
    $project = entity_ui_form_submit_build_entity($form, $form_state);
    $project->save();
    $project_uri = entity_uri('ftep_project', $project);
    drupal_set_message(t('project %title saved.', array('%title' => entity_label('ftep_project', $project))));
    $form_state['redirect'] = $project_uri['path'];
}

/**
 * Delete confirmation form.
 */
function ftep_project_delete_form($form, &$form_state, $ftep4) {
    $form_state['ftep4'] = $ftep4;
    // Always provide entity id in the same form key as in the entity edit form.
    $form['ftep4_type_id'] = array('#type' => 'value', '#value' => entity_id('ftep_project' ,$ftep4));
    $ftep4_uri = entity_uri('ftep_project', $ftep4);
    return confirm_form($form,
        t('Are you sure you want to delete ftep4 %title?', array('%title' => entity_label('ftep_project', $ftep4))),
        $ftep4_uri['path'],
        t('This action cannot be undone.'),
        t('Delete'),
        t('Cancel')
    );
}

/**
 * Delete form submit handler.
 */
function ftep_project_delete_form_submit($form, &$form_state) {
    $ftep4 = $form_state['ftep4'];
    ftep_project_delete($ftep4);

    drupal_set_message(t('ftep4 %title deleted.', array('%title' => entity_label('ftep_project', $ftep4))));

    $form_state['redirect'] = '<front>';
}

