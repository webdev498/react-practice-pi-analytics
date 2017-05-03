// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
// @flow
//jshint esversion:6
import React from "react";
import "react-mdl/extra/material.js";
import "../styles/main.scss";
import FirmDisplay from "./FirmDisplay";
import {Button, Cell, Content, Footer, FooterSection, Grid, Header, HeaderRow, Icon, Layout, ProgressBar} from "react-mdl";
import AddressesContainer from "../containers/AddressesContainer";
import CreateFirmDialogContainer from "../containers/CreateFirmDialogContainer";

type
RootProps = {
  firm: Object,
  isCreateFirmDialogOpen: boolean,
  loading: boolean,
  onCreateFirm: (event: Event) => void,
  onDismiss: (event: Event) => void,
  onSkip: (event: Event) => void,
  onUndo: (event: Event) => void,
  onGetNextServiceAddress: () => void
}

class Root extends React.Component {
  props: RootProps;

  constructor(props: RootProps) {
    super(props);
  }

  componentDidMount() {
    if (!this.props.firm) {
      this.props.onGetNextServiceAddress();
    }
  }

  render() {
    return (
      <Layout fixedHeader>
        <Header>
          <HeaderRow title="Sort Entities into Firms">
          </HeaderRow>
        </Header>
        { !this.props.loading ? (
          <Content style={{padding: "32px"}}>
            <FirmDisplay firm={this.props.firm}/>
            <Grid className="button-bar">
              <Cell col={10}>
                <Button raised onClick={this.props.onCreateFirm}><Icon name="create"/> Create As New Firm</Button>
                <Button raised onClick={this.props.onDismiss}><Icon name="not_interested"/> Not a Law Firm</Button>
                <Button raised onClick={this.props.onSkip}><Icon name="skip_next"/> Skip</Button>
              </Cell>
              <Cell col={2}>
                <Button disabled accent style={{float: "right"}}><Icon name="description"/> View Sample Applications</Button>
              </Cell>
            </Grid>
            <AddressesContainer/>
            <CreateFirmDialogContainer/>
          </Content>
        ) : (
            <Content style={{padding: "32px"}}>
              <ProgressBar indeterminate/>
            </Content>
          )
        }
        {!this.props.loading ? (
          <Footer size="mini">
            <FooterSection type="left"><span>Assigned [1427571] PINCHAK, George, L. Tarolli, Sundheim, Covell & Tummino L.L.P. 1300 East Ninth
      Street, Suite 1700 Cleveland, OH 44114 (US) to Tarolli Sundheim Covell & Tummino, OH</span>
            </FooterSection>
            <FooterSection type="right">
              <Button onClick={this.props.onUndo} raised style={{float: "right"}}>Undo</Button>
            </FooterSection>
          </Footer>
        ) : (
           <Footer/>
         )}
      </Layout>
    )
  }

}

export default Root
