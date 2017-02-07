class ftep::monitor(){

  require ::ftep::globals
  require ::epel

  contain ::ftep::monitor::grafana
  contain ::ftep::monitor::influxdb
  contain ::ftep::monitor::telegraf

}

