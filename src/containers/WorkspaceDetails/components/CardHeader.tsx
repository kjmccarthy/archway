import * as React from 'react';
import { Icon } from 'antd';
import { Colors } from '../../../components';

interface Props {
  icon?: string;
  heading: any;
  subheading: any;
  rightAction?: any;
}

const CardHeader = ({ icon, heading, subheading, rightAction }: Props) => (
  <div style={{ display: 'flex', alignItems: 'center' }}>
    {icon && <Icon
        theme="twoTone"
        twoToneColor={Colors.Green.string()}
        type={icon}
        style={{ paddingRight: 5, fontSize: 32 }} />}
    <div style={{ flex: 1 }}>
      <div style={{
            textTransform: 'uppercase',
            letterSpacing: 1,
            fontWeight: 200,
          }}>
        {heading}
      </div>
      <div style={{
            textTransform: 'uppercase',
            letterSpacing: 1,
            fontWeight: 200,
            fontSize: 10,
          }}>
        {subheading}
      </div>
    </div>
    {rightAction}
  </div>
);

export default CardHeader;
