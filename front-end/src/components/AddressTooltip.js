// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import MDLComponent from "react-mdl/src/utils/MDLComponent";

const AddressTooltip = (props: Object) => {
  const { label, children, ...otherProps } = props;
  const id = Math.random().toString(36).substr(2);

  const newLabel = (typeof label === 'string')
    ? <span>{label}</span>
    : label;

  let element;
  if (typeof children === 'string') {
    element = <span>{children}</span>;
  } else {
    element = React.Children.only(children);
  }

  const width = window.innerWidth

  var positionClass = ""
  if (width < 900) {
    positionClass = 'mdl-tooltip--media-medium'
  }
  if (width < 500){
    positionClass = 'mdl-tooltip--media-small'
  }

  return (
    <div style={{ display: 'inline-block' }} {...otherProps}>
      {React.cloneElement(element, { id })}
      <MDLComponent>
        {React.cloneElement(newLabel, {
          htmlFor: id,
          className: classNames('mdl-tooltip', 'mdl-tooltip--top', positionClass),
        })}
      </MDLComponent>
    </div>
  );
};

AddressTooltip.propTypes = {
  children: PropTypes.node.isRequired,
  label: PropTypes.node.isRequired
};

export default AddressTooltip;
