// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
// @flow
//jshint esversion:6
import React from "react";
import "react-mdl/extra/material.js";
import "../styles/main.scss";
import FirmDisplay from "./FirmDisplay";
import {Button, Cell, Content, Grid, Header, HeaderRow, Icon, Layout, ProgressBar, Snackbar} from "react-mdl";
import AddressesContainer from "../containers/AddressesContainer";
import CreateFirmDialogContainer from "../containers/CreateFirmDialogContainer";
import UndoFooter from "./UndoFooter";
import PatentApplicationsPanel from "./PatentApplicationsPanel";

type
RootProps = {
  value: Object,
  isCreateFirmDialogOpen: boolean,
  loading: boolean,
  undo: Object,
  errorMessage: string,
  applicationsPanelOpen: boolean,
  onCreateFirm: (event: Event) => void,
  onDismiss: (queueId: string, serviceAddressId: string) => void,
  onSkip: (queueId: string) => void,
  onUndo: (serviceAddressId: string) => void,
  onGetNextServiceAddress: () => void,
  onHideError: () => void,
  onToggleApplicationsPanel: (previous: boolean) => void
}

class Root extends React.Component {
  props: RootProps
  state: Object
  hideSnackbar: () => void

  constructor(props: RootProps) {
    super(props);
    this.hideSnackbar = this.hideSnackbar.bind(this)
  }

  componentDidMount() {
    if (!this.props.value) {
      this.props.onGetNextServiceAddress()
    }
  }

  hideSnackbar() {
    this.props.onHideError();
  }

  render() {
    let content = <Content style={{padding: "32px"}}/>

    if (this.props.value) {

      const apps = this.props.value.samplePatentApps
        ? this.props.value.samplePatentApps.map(
          (app, index) =>
            <li key={index}>
              {app.appNum + (app.applicants ? "&nbsp;(Applicants:&nbsp;" + app.applicants.join(",") + ")" : "") }
            </li>
        )
        : null;

      content = (
        <Content style={{padding: "32px"}}>
          <FirmDisplay value={this.props.value}/>
          <Grid className="button-bar">
            <Cell col={9}>
              <Button raised onClick={this.props.onCreateFirm}><Icon name="create"/> Create As New Firm</Button>
              <Button raised onClick={() => {
                this.props.onDismiss(this.props.value.unsortedServiceAddressQueueItemId,
                                     this.props.value.serviceAddressToSort.serviceAddressId)
              }}><Icon name="not_interested"/> Not a Law Firm</Button>
              <Button raised onClick={() => {
                this.props.onSkip(this.props.value.unsortedServiceAddressQueueItemId)
              }}><Icon name="skip_next"/> Skip</Button>
            </Cell>
            <Cell col={3} style={{textAlign: "right"}}>
              <Button accent onClick={() => this.props.onToggleApplicationsPanel(this.props.applicationsPanelOpen)}><Icon
                name="description"/> View Sample Applications</Button>
            </Cell>
          </Grid>
          <PatentApplicationsPanel open={this.props.applicationsPanelOpen} applications={this.props.value.samplePatentApps}/>
          <AddressesContainer/>
          <CreateFirmDialogContainer/>
        </Content>
      )
    } else if (!this.props.loading) {
      content = (
        <Content style={{padding: "32px", textAlign: "center"}}>
          <h4 className="voffset32" style={{color: "#bbb"}}>There are no unsorted service addresses in your queue. Please
            check
            back later.</h4>
          <Button raised onClick={() => this.props.onGetNextServiceAddress()}><Icon name="refresh"/>Refresh</Button>
        </Content>
      )
    }

    return (
      <Layout fixedHeader>
        <Header>
          <HeaderRow title="Sort Entities into Firms">
          </HeaderRow>
        </Header>
        <ProgressBar style={{visibility: this.props.loading ? "visible" : "hidden"}} indeterminate/>
        {content}
        <Snackbar active={this.props.errorMessage ? true : false} onClick={this.hideSnackbar} onTimeout={this.hideSnackbar}
                  action="Close">
          {this.props.errorMessage}
        </Snackbar>
        <UndoFooter disabled={this.props.loading || !this.props.undo} undo={this.props.undo} onUndo={this.props.onUndo}/>
      </Layout>
    )
  }
}

export default Root
