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
  resize: () => void
  hasTooltip: () => void

  constructor(props: AddressesLineProps) {
    super(props)
    this.state = {tooltip: false}
    this.updateElement = this.updateElement.bind(this)
    this.resize = this.resize.bind(this)
    this.hasTooltip = this.hasTooltip.bind(this)
  }

  hasTooltip() {
    function findAncestor(el, cls) {
      while ((el = el.parentElement) && !el.classList.contains(cls)) {

      }
      return el
    }

    const childRect = this.element.getBoundingClientRect()
    const parentRect = findAncestor(this.element, "address-line").getBoundingClientRect()

    return childRect.width + 10 > parentRect.width
  }

  updateElement(element) {
    if (!element) {
      return;
    }
    this.element = element
  }

  resize() {
    this.setState({tooltip: this.hasTooltip()})
  }

  componentDidMount() {
    this.setState({tooltip: this.hasTooltip()})
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