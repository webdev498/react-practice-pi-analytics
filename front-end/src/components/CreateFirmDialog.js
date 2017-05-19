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
import "../styles/main.scss";
import SelectField from "./Selectfield";

const usStates = {
  "AL": "Alabama",
  "AK": "Alaska",
  "AS": "American Samoa",
  "AZ": "Arizona",
  "AR": "Arkansas",
  "CA": "California",
  "CO": "Colorado",
  "CT": "Connecticut",
  "DE": "Delaware",
  "DC": "District Of Columbia",
  "FM": "Federated States Of Micronesia",
  "FL": "Florida",
  "GA": "Georgia",
  "GU": "Guam",
  "HI": "Hawaii",
  "ID": "Idaho",
  "IL": "Illinois",
  "IN": "Indiana",
  "IA": "Iowa",
  "KS": "Kansas",
  "KY": "Kentucky",
  "LA": "Louisiana",
  "ME": "Maine",
  "MH": "Marshall Islands",
  "MD": "Maryland",
  "MA": "Massachusetts",
  "MI": "Michigan",
  "MN": "Minnesota",
  "MS": "Mississippi",
  "MO": "Missouri",
  "MT": "Montana",
  "NE": "Nebraska",
  "NV": "Nevada",
  "NH": "New Hampshire",
  "NJ": "New Jersey",
  "NM": "New Mexico",
  "NY": "New York",
  "NC": "North Carolina",
  "ND": "North Dakota",
  "MP": "Northern Mariana Islands",
  "OH": "Ohio",
  "OK": "Oklahoma",
  "OR": "Oregon",
  "PW": "Palau",
  "PA": "Pennsylvania",
  "PR": "Puerto Rico",
  "RI": "Rhode Island",
  "SC": "South Carolina",
  "SD": "South Dakota",
  "TN": "Tennessee",
  "TX": "Texas",
  "UT": "Utah",
  "VT": "Vermont",
  "VI": "Virgin Islands",
  "VA": "Virginia",
  "WA": "Washington",
  "WV": "West Virginia",
  "WI": "Wisconsin",
  "WY": "Wyoming"
}

type
CreateFirmDialogProps = {
  value: Object,
  open: boolean,
  loading: boolean,
  queueId: string,
  onSubmit: (firm: Object) => void,
  onClose: () => void
}

class CreateFirmDialog extends React.Component {
  props: CreateFirmDialogProps;
  state: Object;
  initialState: Object;
  nameInput: any;

  constructor(props: CreateFirmDialogProps) {
    super(props);
    this.initialState = {
      name: "",
      websiteUrl: "",
      state: "",
      countryCode: this.props.value.serviceAddressToSort.country,
      unsortedServiceAddressQueueItemId: this.props.value.unsortedServiceAddressQueueItemId,
      serviceAddress: this.props.value.serviceAddressToSort
    };
    this.state = u({}, this.initialState);
  }

  componentWillReceiveProps(nextProps: CreateFirmDialogProps) {
    if (!this.props.open && nextProps.open) {
      this.setState(u({}, this.initialState));
    }
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

  handleClickOutside = (event: Event) => {
    if (this.props.open) {
      this.setState({name: "", websiteUrl: "", state: ""});
      this.props.onClose();
    }
  }

  isValid = (): boolean => {
    return this.state.name && (this.state.state || this.state.countryCode != "US")
  }

  render(): ?React.Element<any> {

    const close = (event: Event) => {
      if (event) {
        event.preventDefault();
      }
      this.setState({name: "", websiteUrl: "", state: ""});
      this.props.onClose();
    }

    const options = this.props.value.serviceAddressToSort.country == "US" ? Object.entries(usStates).map(([key, value]) => ({
      value: key,
      name: key + " - " + new String(value).toString()
    })) : null;

    const progress = this.props.loading ? (<ProgressBar indeterminate/>) : null;

    return (
      <Dialog open={this.props.open} onCancel={close}>
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
                <Textfield value={this.state.name} disabled={this.props.loading} style={{width: "100%"}} id={"name"}
                           label={"Name"} ref={(input) => {
                  this.nameInput = input;
                }} onChange={this.createListener('name')}/>
              </Cell>
            </Grid>
            <Grid>
              <Cell col={12}>
                <Textfield value={this.state.websiteUrl} disabled={this.props.loading} style={{width: "100%"}} id={"url"}
                           label={"Firm URL"}
                           onChange={this.createListener('websiteUrl')}/>
              </Cell>
            </Grid>
            <Grid>
              <Cell col={6}>
                <Textfield readOnly={true} label={"Select country"} value={this.props.value.serviceAddressToSort.country}
                           onChange={this.createListener('countryCode')}/>
              </Cell>
              <Cell col={6} style={{textAlign: "right"}}>
                <SelectField value={this.state.state}
                             defaultOption={this.props.value.serviceAddressToSort.country == "US" ? "" : "All states"}
                             label="Select state" id="countryState" name="countryState"
                             options={options}
                             onChange={this.createListener("state")}/>
              </Cell>
            </Grid>
          </DialogContent>
          <DialogActions>
            <Button type="button" disabled={this.props.loading} onClick={close}>Cancel</Button>
            <Button type="submit" disabled={this.props.loading || !this.isValid()}>Save</Button>
          </DialogActions>
        </form>
      </Dialog>
    )
  }

}

export default onClickOutside(CreateFirmDialog)
