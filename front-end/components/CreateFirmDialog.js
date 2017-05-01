// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import u from "updeep";
import React from "react";
import ReactDOM from "react-dom";
import "dialog-polyfill/dialog-polyfill.css";
import {Button, Dialog, DialogContent, DialogTitle, IconButton, Textfield} from "react-mdl";
import onClickOutside from "react-onclickoutside";

type
CreateFirmDialogProps = {
  firm: {},
  open: boolean,
  onSubmit: (firm: Object) => void,
  onCancel: () => void,
}

class CreateFirmDialog extends React.Component {
  props: CreateFirmDialogProps;

  constructor(props: CreateFirmDialogProps) {
    super(props);

    this.state = {name: '', url: '', state: '', country: ''};
  }

  componentDidMount() {
    const dialog = ReactDOM.findDOMNode(this);
    if (!dialog.showModal) {
      window.dialogPolyfill.registerDialog(dialog);
    }
  }

  createListener(name: string) {
    return (event) => {
      this.setState(u({[name]: event.target.value}, this.state));
    }
  }

  render(): ?React.Element<any> {

    const statesList = [].map((state) => {
      return (
        <Options value={state}>{state}</Options>
      )
    });

    const countriesList = [].map((country) => {
      return (
        <Options value={country}>{country}</Options>
      )
    })

    return (
      <Dialog open={this.props.open} onCancel={this.props.onCancel}>
        <Button style={{height: 0, width: 0, padding: 0, margin: 0, float: "right", opacity: 0}}>FocusHere</Button>
        <DialogTitle>Alert Details</DialogTitle>
        <IconButton name="close" onClick={this.props.onCancel} style={{float: "right"}}>Close</IconButton>
        <DialogContent>
          <form onSubmit={(event: Event) => {
            event.preventDefault();
            props.onSubmit(this.state);
          }}>
            <Textfield value={this.props.firm.name} onChange={this.createListener('name')}/>
            <Textfield value={this.props.firm.url} onChange={this.createListener('url')}/>
            <SelectField label={"Select state"} value={this.props.firm.state} onChange={this.createListener('state')}>
              {statesList}
            </SelectField>
            <SelectField label={"Select country"} value={this.props.firm.country} onChange={this.createListener('country')}>
              {countriesList}
            </SelectField>
          </form>
        </DialogContent>
      </Dialog>
    )
  }

}

export default onClickOutside(CreateFirmDialog)
