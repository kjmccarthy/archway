import React from 'react';
import { connect } from 'react-redux';
import { Icon, Tooltip, Row, Col, Card, Avatar, Dropdown, Menu } from 'antd';
import Color from 'color';

import Panel from '../Workspaces/Panel';
import ValueDisplay from '../Workspaces/ValueDisplay';

const serviceColor = (service) => {
  switch (service && service.status) {
    case 'GOOD_HEALTH':
      return '#43AA8B'
    case 'CONCERNING_HEALTH':
      return '#FF6F59'
    case 'BAD_HEALTH':
      return '#DB504A'
    default:
      return '#aaa'
  }
}

const serviceText = (service) => {
  switch (service && service.status) {
    case 'GOOD_HEALTH':
      return '"good"'
    case 'CONCERNING_HEALTH':
      return '"concerning"'
    case 'BAD_HEALTH':
      return '"bad"'
    default:
      return 'unknown'
  }
}

const GlowColor = (status) => {
  console.log(status);
  switch (status) {
    case 'GOOD_HEALTH':
      return `0 0 5px 2px ${Color('#43AA8B').hsl().string()}`;
    case 'CONCERNING_HEALTH':
      return `0 0 5px 2px ${Color('#FF6F59').hsl().string()}`;
    case 'BAD_HEALTH':
      return `0 0 5px 2px ${Color('#DB504A').hsl().string()}`;
    default:
      return false
  }
}

const Service = ({ name, color, status, links, rawStatus, index }) => (
  <Card
    style={{ flex: 1, marginLeft: index == 0 ? 0 : 25 }}
    actions={links}>
    <Card.Meta
      title={name}
      description={`${name}'s status is currently ${status}`}
      avatar={
        <Avatar
          size="small"
          alt={`${name}'s status is currently ${status}`}
          style={{ boxShadow: GlowColor(rawStatus), backgroundColor: color }}
          />
      } />
  </Card>
);

const PersonalWorkspace = () => (
  <Card
    title="Your Personal Workspace"
    >
  </Card>
)

const Home = ({ name, displayStatus, color, services }) => {
  const hiveLinks = [
    (<a href={`https://${services && services.HIVESERVER2.host}:10002`}>Hive UI</a>)
  ];
  const hueLinks = [
    (<a href="https://master2.valhalla.phdata.io:8889">Hue UI</a>)
  ]
  const rmLinks = (
    <Menu>
      <Menu.Item>
        <a target="_blank" href="https://worker1.valhalla.phdata.io:8090">master1</a>
      </Menu.Item>
      <Menu.Item>
        <a target="_blank" href="https://worker2.valhalla.phdata.io:8090">master2</a>
      </Menu.Item>
    </Menu>
  )
  const yarnLinks = [
    (<Dropdown overlay={rmLinks}><a href="#" className="ant-dropdown-link">Node Manager UI <Icon type="down" /></a></Dropdown>),
    (<Dropdown overlay={rmLinks}><a href="#" className="ant-dropdown-link">Resource Manager UI <Icon type="down" /></a></Dropdown>),
  ];
  return (
    <div>
      <div style={{ padding: 24, background: '#fff', textAlign: 'center', height: '100%' }}>
        <h1 style={{ fontWeight: 100  }}>
          You are currently connected to {name}!
        </h1>
        <h3 style={{ fontWeight: 100 }}>
          The current status of {name} is <span style={{ fontWeight: 'bold', color }}>{displayStatus}</span>
        </h3>
        <h2>
          <a href="http://master1.jotunn.io:7180/">
            {name}&apos;s Cloudera Manager UI
          </a>
        </h2>
      </div>
      <div style={{ display: 'flex', marginTop: 25 }}>
        <Service name="Hive" index={0} rawStatus={services && services.HIVESERVER2.status} status={serviceText(services && services.HIVESERVER2)} color={serviceColor(services && services.HIVESERVER2)} links={hiveLinks} />
        <Service name="Hue" index={1} rawStatus={services && services.HUE.status} status={serviceText(services && services.HUE)} color={serviceColor(services && services.HUE)} links={hueLinks} />
        <Service name="Yarn" index={2} rawStatus={services && services.YARN.status} status={serviceText(services && services.YARN)} color={serviceColor(services && services.YARN)} links={yarnLinks} />
      </div>
      <div style={{ marginTop: 25 }}>
        <PersonalWorkspace />
      </div>
    </div>
  );
}

export default connect(
  s => s.cluster, {}
)(Home);
