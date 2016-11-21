/**
 * @ngdoc function
 * @name ftepApp.controller:HelpdeskCtrl
 * @description
 * # HelpdeskCtrl
 * Controller of the ftepApp
 */
define(['../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.controller('HelpdeskCtrl', function ($scope, $http) {

        $scope.result = 'hidden';
        $scope.submitButtonDisabled = false;
        $scope.submitted = false; //used so that form errors are shown only after the form has been submitted
        $scope.submit = function (contactform) {
            $scope.submitted = true;
            $scope.submitButtonDisabled = true;
            if (contactform.$valid) {
                $http({
                    method: 'POST',
                    url: 'contact-form.php',
                    data: $.param($scope.formData), //param method from jQuery
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    } //set the headers so angular passing info as form data (not request payload)
                }).success(function (data) {
                    console.log(data);
                    if (data.success) { //success comes from the return json object
                        $scope.submitButtonDisabled = true;
                        $scope.resultMessage = data.message;
                        $scope.result = 'bg-success';
                    } else {
                        $scope.submitButtonDisabled = false;
                        $scope.resultMessage = data.message;
                        $scope.result = 'bg-danger';
                    }
                });
            } else {
                $scope.submitButtonDisabled = false;
                $scope.resultMessage = 'Failed! Please fill out all the fields.';
                $scope.result = 'bg-danger';
            }
        };
    });
});

//for PHP Mailer
/*
<?php
    require_once 'phpmailer/PHPMailerAutoload.php';

    if (isset($_POST['inputName']) && isset($_POST['inputEmail']) && isset($_POST['inputSubject']) && isset($_POST['inputMessage'])) {

        //check if any of the inputs are empty
        if (empty($_POST['inputName']) || empty($_POST['inputEmail']) || empty($_POST['inputSubject']) || empty($_POST['inputMessage'])) {
            $data = array('success' => false, 'message' => 'Please fill out the form completely.');
            echo json_encode($data);
            exit;
        }

        //create an instance of PHPMailer
        $mail = new PHPMailer();

        $mail->From = $_POST['inputEmail'];
        $mail->FromName = $_POST['inputName'];
        $mail->AddAddress('something@test.com'); //recipient
        $mail->Subject = $_POST['inputSubject'];
        $mail->Body = "Name: " . $_POST['inputName'] . "\r\n\r\nMessage: " . stripslashes($_POST['inputMessage']);

        if (isset($_POST['ref'])) {
            $mail->Body .= "\r\n\r\nRef: " . $_POST['ref'];
        }

        if(!$mail->send()) {
            $data = array('success' => false, 'message' => 'Message could not be sent. Mailer Error: ' . $mail->ErrorInfo);
            echo json_encode($data);
            exit;
        }

        $data = array('success' => true, 'message' => 'Thanks! We have received your message.');
        echo json_encode($data);

    } else {

        $data = array('success' => false, 'message' => 'Please fill out the form completely.');
        echo json_encode($data);

    }
*/
