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
      <h5>WILSON, Mark, Law Offices of Mark Wilson PMB: 348 2530 Berryessa Road San Jose, CA 95132</h5>
      <DataTable style={{width: "100%"}}
                 rowKeyColumn="lawFirmId"
                 rows={[
                   {
                     entity: <span>WILSON, Mark <IconButton accent name="search" /></span>,
                     addressLine1: <span>Law Offices of Mark Wilson <IconButton accent name="search" /></span>,
                     addressLine2: "PMB: 348 2530 Berryessa Road",
                     addressLine3: "San Jose, CA 95132",
                     addressLine4: "",
                     addressLine5: "",
                     phone: "",
                     country: <span>US <IconButton accent name="edit" /></span>,
                     serviceAddressId: "1326958"
                   }]
                 }
                 >
        <TableHeader name="entity">Name</TableHeader>
        <TableHeader name="addressLine1">Address</TableHeader>
        <TableHeader name="addressLine2" />
        <TableHeader name="addressLine3" />
        <TableHeader name="addressLine4" />
        <TableHeader name="addressLine5" />
        <TableHeader name="phone">Phone</TableHeader>
        <TableHeader name="country">Country</TableHeader>
        <TableHeader name="serviceAddressId">Entity ID</TableHeader>
      </DataTable>
      <Button raised style={{margin: "10px 10px 0 0"}}><Icon name="create" /> Create As New Firm</Button>
      <Button raised style={{margin: "10px 10px 0 0"}}><Icon name="not_interested" /> Not a Law Firm</Button>
      <Button raised style={{margin: "10px 10px 0 0"}}><Icon name="skip_next" /> Skip</Button>
      <Button disabled accent style={{margin: "10px 10px 0 0", float: "right"}}><Icon name="description" /> View Sample Applications</Button>

      <Tabs ripple style={{marginTop: "8px"}}>
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
                       serviceAddress: "WILSON, Mark, Law Offices of Mark Wilson PMB: 348 2530 Berryessa Road San Jose, CA 95132",
                       website: <a href="#">http://www.wilsonlawaz.com/mark-wilson/</a>,
                       serviceAddressId: "1036990",
                       actions: <IconButton name="cancel" accent />,
                     },
                     {
                       lawFirmId: <span style={{color: "#bbb"}}>Non law firm</span>,
                       entity: <a href="#">NEL R. FONTANILLA</a>,
                       serviceAddress: "1536 HEMMINGWAY ROAD, SAN JOSE CA 95132",
                       serviceAddressId: "1051425",
                       actions: <IconButton name="cancel" accent />,
                     },
                     {
                       lawFirmId: <span style={{color: "#bbb"}}>Non law firm</span>,
                       entity: <a href="#">INTERACTIV CORPORATION</a>,
                       serviceAddress: "1659 N CAPITOL AVE # 225, 1659 N CAPITOL AVE # 225, SAN JOSE, CA 95132",
                       website: "",
                       serviceAddressId: "563472",
                       actions: <IconButton name="cancel" accent />,
                     },
                     {
                       lawFirmId: <span style={{color: "#bbb"}}>Non law firm</span>,
                       entity: <a href="#">BEAUTYQQ INC</a>,
                       serviceAddress: "2928 LAMBETH COURT, SAN JOSE, CA 95132",
                       website: "",
                       serviceAddressId: "584214",
                       actions: <IconButton name="cancel" accent />,
                     },
                     {
                       lawFirmId: <span>44161 <IconButton accent name="open_in_new" /></span>,
                       entity: <a href="#">PEREZ, YVONNE, CA</a>,
                       serviceAddress: "PEREZ, YVONNE, 2121 LIMEWOOD DR., SAN JOSE, CA 95132",
                       website: "",
                       serviceAddressId: "610070",
                       actions: <IconButton name="cancel" accent />,
                     },
                   ]}
          >
          <TableHeader name="lawFirmId">Law Firm ID</TableHeader>
          <TableHeader name="entity">Entity</TableHeader>
          <TableHeader name="serviceAddress">Address</TableHeader>
          <TableHeader name="website">Website</TableHeader>
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
