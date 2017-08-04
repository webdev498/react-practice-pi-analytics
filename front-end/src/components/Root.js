// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
// @flow
//jshint esversion:6
import React from "react";
import "react-mdl/extra/material.js";
import "../styles/main.scss";
import FirmDisplay from "./FirmDisplay";
import {Button, Cell, Content, Grid, Header, HeaderRow, Icon, Layout, ProgressBar, Snackbar} from "react-mdl";
import SuggestedAgentsContainer from "../containers/SuggestedAgentsContainer";
import CreateLawFirmDialogContainer from "../containers/CreateLawFirmDialogContainer";
import UndoFooter from "./UndoFooter";
import PatentApplicationsPanel from "./PatentApplicationsPanel";

type
RootProps = {
  value: Object,
  isCreateFirmDialogOpen: boolean,
  loading: boolean,
  undo: Object,
  message: Object,
  applicationsPanelOpen: boolean,
  onCreateFirm: (event: Event) => void,
  onSetAsNonLawFirm: (serviceAddressId: string) => void,
  onSetInsufficientInfoStatus: (serviceAddressId: string) => void,
  onSkip: (serviceAddressId: string) => void,
  onUndo: (serviceAddressId: string) => void,
  onGetNextServiceAddress: () => void,
  onHideSnackbar: () => void,
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
    this.props.onHideSnackbar();
  }

  render() {
    let content = <Content style={{padding: "32px"}}/>

    if (this.props.value) {
      const serviceAddressId = this.props.value.serviceAddressToSort.serviceAddressId;

      content = (
        <Content>
          <FirmDisplay value={this.props.value}/>
          <div className="button-bar">
              <Button raised onClick={this.props.onCreateFirm}><Icon name="create"/> Create As New Firm</Button>
              <Button raised onClick={() => this.props.onSetAsNonLawFirm(serviceAddressId)}>
                <Icon name="not_interested"/> Not a Law Firm
              </Button>
              <Button raised onClick={() => this.props.onSetInsufficientInfoStatus(serviceAddressId)}>
                <Icon name="highlight_off"/> Sorting Impossible
              </Button>
              <Button raised onClick={() => {this.props.onSkip(serviceAddressId)}}><Icon name="skip_next"/> Skip</Button>
              <Button accent onClick={() => this.props.onToggleApplicationsPanel(this.props.applicationsPanelOpen)}><Icon
                name="description"/> View Sample Applications</Button>
          </div>
          <PatentApplicationsPanel open={this.props.applicationsPanelOpen} applications={this.props.value.samplePatentApps}/>
          <SuggestedAgentsContainer/>
          <CreateLawFirmDialogContainer/>
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
        <Snackbar className={this.props.message && this.props.message.error ? "error-message" : "info-message"}
                  active={this.props.message ? true : false} onClick={this.hideSnackbar} onTimeout={this.hideSnackbar}
                  timeout={3000}
                  action="&times;">
          {this.props.message ? this.props.message.text : ""}
        </Snackbar>
        <UndoFooter disabled={this.props.loading || !this.props.undo} undo={this.props.undo} onUndo={this.props.onUndo}/>
      </Layout>
    )
  }
}

export default Root
