<?php 


/**
 * Implements hook_form().
 */
function ftep_group_form($form, &$form_state, $group = NULL) {
    $form = array();

    $form['name'] = array(
            '#title' => t('name'),
            '#type' => 'textfield',
            '#default_value' => isset($group->name) ? $group->name : '',
            '#description' => t('Name assigned to Group.'),
            '#required' => TRUE,
            '#maxlength' => 255,
    );

    $form['description'] = array(
            '#title' => t('Description'),
            '#type' => 'textfield',
            '#default_value' => isset($group->description) ? $group->description : '',
            '#required' => TRUE,
            '#maxlength' => 255,
    );
    

    field_attach_form('group', $group, $form, $form_state);
    
    $form['actions'] = array(
            '#type' => 'actions',
            'submit' => array(
                    '#type' => 'submit',
                    '#value' => isset($group->gid) ? t('Update Group') : t('Save Group'),
            ),
            'delete_link' => array(
                    '#markup' => isset($group->gid) ? l(t('Delete'), 'admin/content/group/manage/' . $group->gid . '/delete', array('attributes' => array('gid' => array('group-delete-' . $group->gid), 'class' => array('button remove')), 'query' => array('destination' => 'admin/content/groups'))) : ''));
            return $form;
}
/**
 * Implements hook_form_validate().
 */
function ftep_group_form_validate($form, &$form_state) {
}


/**
 * Implements hook_form_submit().
 */
function ftep_group_form_submit($form, &$form_state) {
    $group = entity_ui_form_submit_build_entity($form, $form_state);
    $group->save();
    drupal_set_message(t('@name group has been saved.', array('@name' => $group->name)));
    // $form_state['redirect'] = 'admin/content/lawmakers';
    $form_state['redirect'] = 'groups/' . $group->gid;
}

