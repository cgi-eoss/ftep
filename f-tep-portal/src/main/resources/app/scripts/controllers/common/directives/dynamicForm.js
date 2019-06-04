define(['../../../ftepmodules'], function (ftepmodules) {
    'use strict';

    ftepmodules.directive('dynamicForm', ['moment', function(moment) {
        return {
            scope: {
                formConfig: '=',
                api: '=',
                formData: '='
            },
            restrict: 'E',
            link: function(scope, element, attrs) {

                scope.formData = scope.formData || {};
                scope.enabledFields = [];

                scope.api = {
                    getFormData: function() {
                        var data = {}
                        for (var field in scope.formData) {
                            if (scope.enabledFields[field]) {
                                var type = scope.formConfig[field].type;
                                if (type === 'daterange') {
                                    data[field + 'Start'] = moment(scope.formData[field].start).format(scope.formConfig[field].startFormat || 'YYYY-MM-DD[T00:00:00Z]');
                                    data[field + 'End'] = moment(scope.formData[field].end).format(scope.formConfig[field].endFormat || 'YYYY-MM-DD[T23:59:59Z]');
                                }
                                else if (type === 'date') {
                                    data[field] = moment(scope.formData[field]).format(scope.formConfig[field].format || 'YYYY-MM-DD[T00:00:00Z]')
                                }
                                else {
                                    data[field] = scope.formData[field];
                                }
                            }
                        }

                        return data;
                    }
                }


                scope.allowedSelectValues = {};

                scope.setDefaultValue = function(fieldId, fieldConfig) {
                    if(fieldConfig.defaultValue) {
                        if(fieldConfig.type === 'text' || fieldConfig.type === 'int' || fieldConfig.type === 'polygon') {
                            scope.formData[fieldId] = fieldConfig.defaultValue;
                        } else if(fieldConfig.type === 'select') {
                            for (var item in fieldConfig.allowed.values) {
                                if(fieldConfig.defaultValue === fieldConfig.allowed.values[item].value) {
                                    scope.formData[fieldId] = fieldConfig.allowed.values[item].value;
                                }
                            }
                        } else if(fieldConfig.type === 'daterange') {

                            scope.formData[fieldId] = {};

                            var startPeriod = new Date();
                            startPeriod.setMonth(startPeriod.getMonth() + parseInt(fieldConfig.defaultValue[0]));
                            scope.formData[fieldId].start = startPeriod;

                            var endPeriod = new Date();
                            endPeriod.setMonth(endPeriod.getMonth() + parseInt(fieldConfig.defaultValue[1]));
                            scope.formData[fieldId].end = endPeriod;

                        } else if (fieldConfig.type === 'date') {
                            var dt = new Date();
                            dt.setMonth(dt.getMonth() + parseInt(fieldConfig.defaultValue[0]));
                            scope.formData[fieldId] = dt;
                        }
                    }
                };

                scope.displayField = function(fieldConfig) {
                    if (!fieldConfig.onlyIf) {
                        return true;
                    } else {
                        for (var condition in fieldConfig.onlyIf) {
                            for (var item in scope.formData) {
                                if (item === condition) {
                                    for (var value in fieldConfig.onlyIf[condition]) {
                                        if(fieldConfig.onlyIf[condition][value] === scope.formData[item]) {
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                scope.updateFormDataValues = function() {
                    for(var fieldId in scope.formConfig) {

                        // Remove values for fields no longer displayed
                        if(!scope.displayField(scope.formConfig[fieldId])) {
                            delete scope.formData[fieldId];
                            delete scope.enabledFields[fieldId];
                        }

                        scope.enabledFields[fieldId] = scope.formConfig[fieldId].optional ? (scope.enabledFields[fieldId] || false): true;

                        if (scope.formConfig[fieldId].type === 'select') {
                            // Get list of allowed values
                            var allowedValues = getAllowedSelectValues(scope.formConfig[fieldId]);
                            scope.allowedSelectValues[fieldId] = allowedValues;
                            // Clear any fields set with an invalid value
                            removeInvalidValues(fieldId, allowedValues);
                        }
                    }
                };


                /* For all values*/
                function getAllowedSelectValues(field) {
                    var displayValues = [];
                    var allFieldValues = field.allowed.values;

                    for (var value in allFieldValues) {
                        // If value is not dependant on another add to list
                        if (!allFieldValues[value].onlyIf) {
                            displayValues.push(allFieldValues[value]);
                        // If value depends on anothers
                        } else {
                            for (var depField in allFieldValues[value].onlyIf) {
                                var allowedValues = allFieldValues[value].onlyIf[depField];
                                for (var item in allowedValues) {
                                    if(scope.formData[depField]) {
                                        if(scope.formData[depField] === allowedValues[item]) {
                                            displayValues.push(allFieldValues[value]);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    return displayValues;
                }

                function removeInvalidValues(fieldId, allowedValues) {
                    var match = false;
                    if(scope.formData[fieldId]) {
                        for (var v in allowedValues) {
                            if(scope.formData[fieldId] === allowedValues[v].value) {
                                match = true;
                            }
                        }
                    }
                    if (!match) {
                        delete scope.formData[fieldId];
                    }
                }


                scope.getSelectValueDescription = function(fieldId) {
                    var values = scope.allowedSelectValues[fieldId];
                    for (var i=0; i < values.length; ++i) {
                        if (values[i].value == scope.formData[fieldId]) {
                            return values[i].description;
                        }
                    }
                };

            },
            templateUrl: 'views/common/directives/dynamicForm.html'
        };
    }]);
});