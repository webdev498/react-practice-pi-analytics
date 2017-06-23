// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import React from "react";
import {Tooltip} from "react-mdl";

type
AddressesLineProps = {
  text: string,
  fullText: string
}

class AddressesLine extends React.Component {
  props: AddressesLineProps
  element: any
  state: Object
  updateElement: () => void

  constructor(props: AddressesLineProps) {
    super(props)
    this.state = {tooltip: false}
    this.updateElement = this.updateElement.bind(this)
  }

  updateElement(element) {
    if (!element) {
      return;
    }
    this.element = element;
  }

  resize = () => this.forceUpdate()

  componentDidMount() {
    const parentRect = this.element.parentElement.getBoundingClientRect();
    const childRect = this.element.getBoundingClientRect();

    this.setState({tooltip: childRect.width > parentRect.width});
    window.addEventListener('resize', this.resize)
  }

  componentWillUnmount() {
    window.removeEventListener('resize', this.resize)
  }

  render() {
    if (this.state.tooltip) {
      return (
        <Tooltip label={this.props.fullText} position="top">
          <span ref={this.updateElement}
                dangerouslySetInnerHTML={{__html: this.props.text ? this.props.text : "N/A"}}/>
        </Tooltip>
      )
    } else {
      return (
        <span ref={this.updateElement}
              dangerouslySetInnerHTML={{__html: this.props.text ? this.props.text : "N/A"}}/>
      )
    }
  }

}

export default AddressesLine