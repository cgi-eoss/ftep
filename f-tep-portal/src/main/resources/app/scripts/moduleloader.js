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
    './controllers/community/sidebar/databaskets',
    './controllers/community/sidebar/files',
    './controllers/community/sidebar/groups',
    './controllers/community/sidebar/jobs',
    './controllers/community/sidebar/projects',
    './controllers/community/sidebar/services',
    './controllers/community/manage/databasket',
    './controllers/community/manage/file',
    './controllers/community/manage/group',
    './controllers/community/manage/job',
    './controllers/community/manage/project',
    './controllers/community/manage/service',

    './controllers/developer/developer',
    './controllers/developer/service',

    './controllers/explorer/explorer',
    './controllers/explorer/map',
    './controllers/explorer/sidebar/project',
    './controllers/explorer/sidebar/search',
    './controllers/explorer/sidebar/services',
    './controllers/explorer/sidebar/workspace',

    './controllers/helpdesk/helpdesk',

    //services
    './services/basketservice',
    './services/commonservice',
    './services/communityservice',
    './services/fileservice',
    './services/groupservice',
    './services/jobservice',
    './services/mapservice',
    './services/messageservice',
    './services/projectservice',
    './services/productservice',
    './services/publishingservice',
    './services/searchservice',
    './services/tabservice',
    './services/userservice',
    './services/walletservice'

], function () {});
