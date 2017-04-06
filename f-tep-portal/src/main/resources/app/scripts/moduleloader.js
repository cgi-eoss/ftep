define([
    //contollers

    './controllers/index',
    './controllers/navbar',

    './controllers/admin/admin',

    './controllers/account/account',

    './controllers/common/messages',
    './controllers/common/bottombar/bottombar',
    './controllers/common/bottombar/databasket',
    './controllers/common/bottombar/jobs',
    './controllers/common/bottombar/results',

    './controllers/community/community',
    './controllers/community/common/databaskets',
    './controllers/community/common/files',
    './controllers/community/common/groups',
    './controllers/community/common/jobs',
    './controllers/community/common/share',
    './controllers/community/common/projects',
    './controllers/community/common/services',
    './controllers/community/manage/databasket',
    './controllers/community/manage/file',
    './controllers/community/manage/group',
    './controllers/community/manage/job',
    './controllers/community/manage/project',
    './controllers/community/manage/service',
    './controllers/community/share/databasket',
    './controllers/community/share/file',
    './controllers/community/share/group',
    './controllers/community/share/job',
    './controllers/community/share/project',
    './controllers/community/share/service',

    './controllers/developer/developer',
    './controllers/developer/service',

    './controllers/explorer/explorer',
    './controllers/explorer/map',
    './controllers/explorer/sidebar/project',
    './controllers/explorer/sidebar/search',
    './controllers/explorer/sidebar/services',
    './controllers/explorer/sidebar/sidebar',
    './controllers/explorer/sidebar/workspace',

    './controllers/helpdesk/helpdesk',

    //services
    './services/basketservice',
    './services/commonservice',
    './services/communityservice',
    './services/fileservice',
    './services/geoservice',
    './services/groupservice',
    './services/jobservice',
    './services/mapservice',
    './services/messageservice',
    './services/projectservice',
    './services/productservice',
    './services/tabservice',
    './services/userservice',
    './services/wpsservice'

], function () {});
