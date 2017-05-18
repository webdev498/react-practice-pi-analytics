// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
// @flow
//jshint esversion:6
import React from "react";
import "react-mdl/extra/material.js";
import "../styles/main.scss";
import FirmDisplay from "./FirmDisplay";
import {Button, Cell, Content, Grid, Header, HeaderRow, Icon, Layout, ProgressBar} from "react-mdl";
import AddressesContainer from "../containers/AddressesContainer";
import CreateFirmDialogContainer from "../containers/CreateFirmDialogContainer";
import UndoFooter from "./UndoFooter";

type
RootProps = {
  value: Object,
  isCreateFirmDialogOpen: boolean,
  loading: boolean,
  undo: Object,
  onCreateFirm: (event: Event) => void,
  onDismiss: (queueId: string, serviceAddressId: string) => void,
  onSkip: (queueId: string) => void,
  onUndo: (serviceAddressId: string) => void,
  onGetNextServiceAddress: () => void
}

class Root extends React.Component {
  props: RootProps

  componentDidMount() {
    if (!this.props.value) {
      this.props.onGetNextServiceAddress()
    }
  }

  render() {
    let content = <Content style={{padding: "32px"}}/>

    if (this.props.value) {
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
              <Button disabled accent><Icon name="description"/> View Sample Applications</Button>
            </Cell>
          </Grid>
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

    const progress = this.props.loading ? (<ProgressBar indeterminate/>) : null

    return (
      <Layout fixedHeader>
        <Header>
          <HeaderRow title="Sort Entities into Firms">
          </HeaderRow>
        </Header>
        {progress}
        {content}
        <UndoFooter disabled={this.props.loading || !this.props.undo} undo={this.props.undo} onUndo={this.props.onUndo}/>
      </Layout>
    )
  }
}

export default Root
