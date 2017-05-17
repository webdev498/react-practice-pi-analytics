// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
import React from "react";
import PropTypes from "prop-types";
import {findDOMNode} from "react-dom";
import classNames from "classnames";
import mdlUpgrade from "react-mdl/lib/utils/mdlUpgrade";
import "mdl-selectfield/dist/mdl-selectfield.css";
import "mdl-selectfield/dist/mdl-selectfield";
import "../styles/components/_selectfield.scss";

const propTypes = {
  className: PropTypes.string,
  disabled: PropTypes.bool,
  error: PropTypes.node,
  floatingLabel: PropTypes.bool,
  id: (props, propName, componentName) => {
    const {id} = props;
    if (id && typeof id !== 'string') {
      return new Error(
        `Invalid prop \`${propName}\` supplied to \`${componentName}\`. \`${propName}\` should be a string. Validation failed.`);
    }
    if (!id && typeof props.label !== 'string') {
      return new Error(
        `Invalid prop \`${propName}\` supplied to \`${componentName}\`. \`${propName}\` is required when label is an element. Validation failed.`);
    }
    return null;
  },
  inputClassName: PropTypes.string,
  label: PropTypes.oneOfType([PropTypes.string, PropTypes.element]).isRequired,
  onChange: PropTypes.func,
  required: PropTypes.bool,
  style: PropTypes.object,
  options: PropTypes.arrayOf(PropTypes.object),
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  defaultOption: PropTypes.string
};

class Selectfield extends React.Component {

  componentDidMount() {
    if (this.props.error && !this.props.pattern) {
      this.setAsInvalid();
    }
  }

  componentDidUpdate(prevProps) {
    if (
      this.props.required !== prevProps.required ||
      this.props.pattern !== prevProps.pattern ||
      this.props.error !== prevProps.error
    ) {
      findDOMNode(this).MaterialSelectfield.checkValidity();
    }
    if (this.props.disabled !== prevProps.disabled) {
      findDOMNode(this).MaterialSelectfield.checkDisabled();
    }
    if (this.props.value !== prevProps.value && this.inputRef !== document.activeElement) {
      findDOMNode(this).MaterialSelectfield.change(this.props.value);
    }
    if (this.props.error && !this.props.pattern) {
      // Every time the input gets updated by MDL (checkValidity() or change())
      // its invalid class gets reset. We have to put it again if the input is specifically set as "invalid"
      this.setAsInvalid();
    }
  }

  setAsInvalid() {
    const elt = findDOMNode(this);
    if (elt.className.indexOf('is-invalid') < 0) {
      elt.className = classNames(elt.className, 'is-invalid');
    }
  }

  render() {
    const {
      className, inputClassName, id, error, floatingLabel, label, style, children, options, value, defaultOption,
      ...otherProps
    } = this.props;

    const customId = id || `selectfield-${label.replace(/[^a-z0-9]/gi, '')}`;

    const inputProps = {
      className: classNames('mdl-selectfield__select', inputClassName),
      id: customId,
      ref: (c) => (this.inputRef = c),
      label,
      value,
      ...otherProps
    };

    const labelContainer = <label className="mdl-selectfield__label" htmlFor={customId}>{label}</label>;
    const errorContainer = !!error && <span className="mdl-selectfield__error">{error}</span>;

    const containerClasses = classNames('mdl-selectfield mdl-js-selectfield', {
      'mdl-selectfield--floating-label': floatingLabel,
    }, className);

    const content = options ? options.map(option => <option key={option.value} value={option.value}>{option.name}</option>)
      : null;

    return (
      <div className={containerClasses} style={style}>
        <select {...inputProps}>
          <option>{defaultOption}</option>
          {content}
        </select>
        <div className="mdl-selectfield__icon">
          <i className="material-icons">arrow_drop_down</i>
        </div>
        {labelContainer}
        {errorContainer}
        {children}
      </div>
    );
  }
}

Selectfield.propTypes = propTypes;

export default mdlUpgrade(Selectfield);
