// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
// @flow
import React from "react"
import "react-mdl/extra/material.js"
import "../styles/main.scss"
import {
  Layout, Header, HeaderRow, Content, Navigation, Button, Icon, IconButton, Tabs, Tab, DataTable, TableHeader, Snackbar
} from "react-mdl"

const Root = (): React.Element<Layout> =>
  <Layout fixedHeader>
    <Header>
      <HeaderRow title="Sort Entities into Firms">
      </HeaderRow>
    </Header>
    <Content style={{padding: "32px"}}>
      <h5>WILSON, Mark, Law Offices of Mark Wilson: 348 2530 Berryessa Road San Jose, CA 95132</h5>
      <DataTable style={{width: "100%"}}
                 rowKeyColumn="lawFirmId"
                 rows={[
                   {
                     entity: <span>WILSON, Mark <IconButton accent name="search" /></span>,
                     address: <span>Law Offices of Mark Wilson PMB: 348 2530 Berryessa Road San Jose, CA 95132<IconButton accent name="search" /></span>,
                     phone: "",
                     country: <span>US <IconButton accent name="edit" /></span>,
                     serviceAddressId: "1326958"
                   }]
                 }
                 >
        <TableHeader name="entity">Name</TableHeader>
        <TableHeader name="address">Address</TableHeader>
        <TableHeader name="phone">Phone</TableHeader>
        <TableHeader name="country">Country</TableHeader>
        <TableHeader name="serviceAddressId">Entity ID</TableHeader>
      </DataTable>
      <Button raised style={{margin: "10px 32px 0 0"}}><Icon name="create" /> Create As New Firm</Button>
      <Button raised style={{margin: "10px 32px 0 0"}}><Icon name="not_interested" /> Not a Law Firm</Button>
      <Button raised style={{margin: "10px 32px 0 0"}}><Icon name="skip_next" /> Skip</Button>
      <Button disabled accent style={{margin: "10px 10px 0 0", float: "right"}}><Icon name="description" /> View Sample Applications</Button>

      <Tabs ripple style={{marginTop: "16px"}}>
        <Tab>Suggestions (US)</Tab>
        <Tab>Search</Tab>
      </Tabs>
      <section>
        <DataTable style={{width: "100%"}}
                   rowKeyColumn="lawFirmId"
                   rows={[
                     {
                       lawFirmId: <span>58586 <IconButton accent name="open_in_new" /></span>,
                       entity: <a href="#">Law Offices Of Mark A. Wilson, CA</a>,
                       serviceAddress: <p><span style={{background: "#ffffbc"}}>WILSON</span>, <span style={{background: "#ffffbc"}}>Mark</span>, <span style={{background: "#ffffbc"}}>Law</span> <span style={{background: "#ffffbc"}}>Offices</span> of <span style={{background: "#ffffbc"}}>Mark</span> <span style={{background: "#ffffbc"}}>Wilson</span> PMB: <span style={{background: "#ffffbc"}}>348</span> <span style={{background: "#ffffbc"}}>2530</span> <span style={{background: "#ffffbc"}}>Berryessa</span> Rd <span style={{background: "#ffffbc"}}>San</span> <span style={{background: "#ffffbc"}}>Jose</span>, <span style={{background: "#ffffbc"}}>CA</span> <span style={{background: "#ffffbc"}}>95132</span><br /><span style={{background: "#ffffbc"}}>Mark</span> <span style={{background: "#ffffbc"}}>Wilson</span> PMB: <span style={{background: "#ffffbc"}}>348</span> <span style={{background: "#ffffbc"}}>2530</span> <span style={{background: "#ffffbc"}}>Berryessa</span> <span style={{background: "#ffffbc"}}>Road</span> <span style={{background: "#ffffbc"}}>San</span> <span style={{background: "#ffffbc"}}>Jose</span>, California <span style={{background: "#ffffbc"}}>95132</span></p>,
                       website: <IconButton name="public" accent />,
                       serviceAddressId: "1036990",
                       actions: <IconButton name="cancel" accent />,
                     },
                     {
                       lawFirmId: <span style={{color: "#bbb"}}>Non law firm</span>,
                       entity: "NEL R. FONTANILLA",
                       serviceAddress: <p>1536 HEMMINGWAY <span style={{background: "#ffffbc"}}>ROAD</span>, <span style={{background: "#ffffbc"}}>SAN</span> <span style={{background: "#ffffbc"}}>JOSE</span> <span style={{background: "#ffffbc"}}>CA</span> <span style={{background: "#ffffbc"}}>95132</span></p>,
                       serviceAddressId: "1051425",
                       actions: <IconButton name="cancel" accent />,
                     },
                     {
                       lawFirmId: <span style={{color: "#bbb"}}>Non law firm</span>,
                       entity: "INTERACTIV CORPORATION",
                       serviceAddress: <p>1659 N CAPITOL AVE # 225, 1659 N CAPITOL AVE # 225, <span style={{background: "#ffffbc"}}>SAN</span> <span style={{background: "#ffffbc"}}>JOSE</span>, <span style={{background: "#ffffbc"}}>CA</span> <span style={{background: "#ffffbc"}}>95132</span></p>,
                       website: "",
                       serviceAddressId: "563472",
                       actions: <IconButton name="cancel" accent />,
                     },
                     {
                       lawFirmId: <span style={{color: "#bbb"}}>Non law firm</span>,
                       entity: "BEAUTYQQ INC",
                       serviceAddress: <p>2928 LAMBETH COURT, <span style={{background: "#ffffbc"}}>SAN</span> <span style={{background: "#ffffbc"}}>JOSE</span>, <span style={{background: "#ffffbc"}}>CA</span> <span style={{background: "#ffffbc"}}>95132</span></p>,
                       website: "",
                       serviceAddressId: "584214",
                       actions: <IconButton name="cancel" accent />,
                     },
                     {
                       lawFirmId: <span>44161 <IconButton accent name="open_in_new" /></span>,
                       entity: <a href="#">PEREZ, YVONNE, CA</a>,
                       serviceAddress: <p>PEREZ, YVONNE, 2121 LIMEWOOD DR., <span style={{background: "#ffffbc"}}>SAN</span> <span style={{background: "#ffffbc"}}>JOSE</span>, <span style={{background: "#ffffbc"}}>CA</span> <span style={{background: "#ffffbc"}}>95132</span></p>,
                       website: "",
                       serviceAddressId: "610070",
                       actions: <IconButton name="cancel" accent />,
                     },
                   ]}
          >
          <TableHeader name="lawFirmId">Law Firm ID</TableHeader>
          <TableHeader name="entity">Firm</TableHeader>
          <TableHeader name="serviceAddress">Service Addresses</TableHeader>
          <TableHeader name="website">www</TableHeader>
          <TableHeader name="serviceAddressId">Entity ID</TableHeader>
          <TableHeader name="actions">Re-sort</TableHeader>
        </DataTable>
      </section>
    </Content>
    <Snackbar active={true} action="Undo" onTimeout={() => {}}>
      Assigned [1427571] PINCHAK, George, L. Tarolli, Sundheim, Covell & Tummino L.L.P. 1300 East Ninth Street, Suite 1700 Cleveland, OH 44114 (US) to Tarolli Sundheim Covell & Tummino, OH
    </Snackbar>
  </Layout>

export default Root
