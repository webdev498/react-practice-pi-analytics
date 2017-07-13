// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import React from "react";
import {Tooltip} from "react-mdl";
import AddressTooltip from "./AddressTooltip";

type
AddressLineProps = {
  text: string,
  fullText: string,
  parentClass: string
}

class AddressesLine extends React.Component {
  props: AddressLineProps
  element: any
  state: Object
  updateElement: () => void
  resize: () => void
  getUpdatedState: () => void

  constructor(props: AddressLineProps) {
    super(props)
    this.state = {tooltip: false}
    this.updateElement = this.updateElement.bind(this)
    this.resize = this.resize.bind(this)
    this.getUpdatedState = this.getUpdatedState.bind(this)
  }

  getUpdatedState() {
    const parentClass = this.props.parentClass || "address-line"
    const elementRect = this.element.getBoundingClientRect()

    var parentElement = this.element;

    while (!parentElement.classList.contains(parentClass)) {
      parentElement = parentElement.parentElement
    }

    const parentElementRect = parentElement.getBoundingClientRect()

    return {tooltip: elementRect.right + 8 > parentElementRect.right};
  }

  updateElement(element: Element) {
    if (!element) {
      return;
    }
    this.element = element
  }

  resize() {
    this.setState(this.getUpdatedState())
  }

  componentDidMount() {
    this.setState(this.getUpdatedState())
    window.addEventListener('resize', this.resize)
  }

  componentWillUnmount() {
    window.removeEventListener('resize', this.resize)
  }

  render() {

    const innerText = this.props.text && this.props.text.length > 0 ? this.props.text : "N/A"

    if (this.state.tooltip) {
      return (
        <AddressTooltip label={this.props.fullText} >
          <span ref={this.updateElement}
                dangerouslySetInnerHTML={{__html: innerText}}/>
        </AddressTooltip>
      )
    } else {
      return (
        <span ref={this.updateElement}
              dangerouslySetInnerHTML={{__html: innerText}}/>
      )
    }
  }

}

export default AddressesLine