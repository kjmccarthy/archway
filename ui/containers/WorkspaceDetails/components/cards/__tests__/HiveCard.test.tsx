import * as React from 'react';
import { shallow } from 'enzyme';
import toJson from 'enzyme-to-json';

import HiveCard from '../HiveCard';
import { HiveAllocation } from '../../../../../models/Workspace';

describe('HiveCard', () => {
  it('renders correctly', () => {
    const props = {
      data: [
        {
          id: 161,
          name: 'sw_test_workspace',
          location: 'hdfs://valhalla/data/shared_workspace/test_workspace',
          size_in_gb: 1000,
          consumed_in_gb: 0,
        } as HiveAllocation,
      ],
    };
    const wrapper = shallow(<HiveCard {...props} />);

    expect(toJson(wrapper)).toMatchSnapshot();
  });
});
