// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
// @flow
import React from "react";
import "react-mdl/extra/material.js";
import "../styles/main.scss";
import FirmDisplay from "./FirmDisplay";
import {Button, Cell, Content, Footer, FooterSection, Grid, Header, HeaderRow, Icon, Layout} from "react-mdl";
import AddressesContainer from "../containers/AddressesContainer";

type
RootProps = {
  firm: Object,
  onCreateFirm: (event: Event) => void,
  onDismiss: (event: Event) => void,
  onSkip: (event: Event) => void,
  onUndo: (event: Event) => void
}

const Root = (props: RootProps): React.Element<Layout> =>
  <Layout fixedHeader>
    <Header>
      <HeaderRow title="Sort Entities into Firms">
      </HeaderRow>
    </Header>
    <Content style={{padding: "32px"}}>
      <FirmDisplay firm={props.firm}/>
      <Grid>
        <Cell col={10} className="button-bar">
          <Button raised onClick={props.onCreateFirm}><Icon name="create"/> Create As New Firm</Button>
          <Button raised onClick={props.onDismiss}><Icon name="not_interested"/> Not a Law Firm</Button>
          <Button raised onClick={props.onSkip}><Icon name="skip_next"/> Skip</Button>
        </Cell>
        <Cell col={2}>
          <Button disabled accent style={{float: "right"}}><Icon name="description"/> View Sample
            Applications</Button>
        </Cell>
      </Grid>

      <AddressesContainer/>
      <Footer size="mini">
        <FooterSection type="left"><span>Assigned [1427571] PINCHAK, George, L. Tarolli, Sundheim, Covell & Tummino L.L.P. 1300 East Ninth
          Street, Suite 1700 Cleveland, OH 44114 (US) to Tarolli Sundheim Covell & Tummino, OH</span>
        </FooterSection>
        <FooterSection type="right">
          <Button onClick={props.onUndo} raised style={{float: "right"}}>Undo</Button>
        </FooterSection>
      </Footer>
    </Content>
  </Layout>

export default Root
