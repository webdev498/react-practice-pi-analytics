// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import u from "updeep";
import React from "react";
import ReactDOM from "react-dom";
import "dialog-polyfill/dialog-polyfill.css";
import * as DialogPolyfill from "dialog-polyfill";
import {
  Button,
  Cell,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  IconButton,
  ProgressBar,
  Textfield
} from "react-mdl";
import onClickOutside from "react-onclickoutside";
import {SelectField} from "react-mdl-selectfield";

type CreateFirmDialogProps = {
  value: Object,
  open: boolean,
  loading: boolean,
  queueId: string,
  onSubmit: (firm: Object) => void,
  onClose: () => void
}

class CreateFirmDialog extends React.Component {
  props: CreateFirmDialogProps;

  constructor(props: CreateFirmDialogProps) {
    super(props);
    this.state = {
      name: '',
      websiteUrl: '',
      state: '',
      countryCode: '',
      unsortedServiceAddressQueueItemId: this.props.value.unsortedServiceAddressQueueItemId
    };
  }

  componentDidMount() {
    const dialog = ReactDOM.findDOMNode(this);
    if (dialog && !dialog.showModal) {
      DialogPolyfill.registerDialog(dialog);
    }
  }

  createListener(name: string) {
    return (event) => {
      this.setState(u({[name]: event.target.value}, this.state));
    }
  }

  createOptions(values: Array<string>) {
    return values.map((value) => {
      return (
        <Options value={value}>{value}</Options>
      )
    });
  }

  handleClickOutside = (event: Event) => {
    if (this.props.open) {
      this.props.onClose();
    }
  }

  render(): ?React.Element<any> {

    let statesList = this.createOptions([]);
    let countriesList = this.createOptions([]);

    let close = (event: Event) => {
      event.preventDefault();
      this.props.onClose();
    }

    let progress = this.props.loading ? (<ProgressBar indeterminate/>) : null;

    return (
      <Dialog open={this.props.open} onCancel={this.props.onClose}>
        <DialogTitle>Create new Law Firm</DialogTitle>
        <IconButton disabled={this.props.loading} name="close" onClick={close} style={{top: "18px"}}
                    onFocus={(event) => this.nameInput.inputRef.focus()}>Close</IconButton>
        {progress}
        <form onSubmit={(event: Event) => {
          event.preventDefault();
          this.props.onSubmit(this.state);
        }}>
          <DialogContent>
            <Grid>
              <Cell col={12}>
                <Textfield disabled={this.props.loading} style={{width: "100%"}} id={"name"} label={"Name"} ref={(input) => {
                  this.nameInput = input;
                }} onChange={this.createListener('name')}/>
              </Cell>
            </Grid>
            <Grid>
              <Cell col={12}>
                <Textfield disabled={this.props.loading} style={{width: "100%"}} id={"url"} label={"Firm URL"}
                           onChange={this.createListener('websiteUrl')}/>
              </Cell>
            </Grid>
            <Grid>
              <Cell col={6}>
                <SelectField disabled={this.props.loading} label={"Select state"} onChange={this.createListener('state')}>
                  {statesList}
                </SelectField>
              </Cell>
              <Cell col={6}>
                <SelectField disabled={this.props.loading} label={"Select country"}
                             onChange={this.createListener('countryCode')}>
                  {countriesList}
                </SelectField>
              </Cell>
            </Grid>
          </DialogContent>
          <DialogActions>
            <Button type="button" disabled={this.props.loading} onClick={close}>Cancel</Button>
            <Button type="submit" disabled={this.props.loading}>Save</Button>
          </DialogActions>
        </form>
      </Dialog>
    )
  }

}

export default onClickOutside(CreateFirmDialog)
