// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
// @flow
import React from "react"
import "react-mdl/extra/material.js"
import "../styles/main.scss"
import {
  Layout, Header, HeaderRow, Content, Navigation, Button, Icon, IconButton, Tabs, Tab, DataTable, TableHeader,
  Footer, FooterSection,
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
        <TableHeader name="entity">Entity</TableHeader>
        <TableHeader name="addressLine1">Address</TableHeader>
        <TableHeader name="addressLine2" />
        <TableHeader name="addressLine3" />
        <TableHeader name="addressLine4" />
        <TableHeader name="addressLine5" />
        <TableHeader name="phone">Phone</TableHeader>
        <TableHeader name="country">Country</TableHeader>
        <TableHeader name="serviceAddressId">Entity ID</TableHeader>
      </DataTable>
      <Button accent style={{margin: "10px 10px 0 0"}}><Icon name="create" /> Create As New Firm</Button>
      <Button accent style={{margin: "10px 10px 0 0"}}><Icon name="not_interested" /> Not a Law Firm</Button>
      <Button accent style={{margin: "10px 10px 0 0"}}><Icon name="skip_next" /> Skip</Button>

      <Tabs ripple style={{marginTop: "50px"}}>
        <Tab>Suggestions</Tab>
        <Tab>Search</Tab>
      </Tabs>
      <section>
        <DataTable style={{width: "100%"}}
                   shaodow={2}
                   rowKeyColumn="lawFirmId"
                   rows={[
                     {
                       lawFirmId: <span>58586 <IconButton accent name="open_in_new" /></span>,
                       entity: "Law Offices Of Mark A. Wilson, CA",
                       serviceAddress: "WILSON, Mark, Law Offices of Mark Wilson PMB: 348 2530 Berryessa Road San Jose, CA 95132",
                       country: "US",
                       website: <a>http://www.wilsonlawaz.com/mark-wilson/</a>,
                       serviceAddressId: "1036990",
                       actions: <IconButton name="cancel" accent />,
                     },
                     {
                       lawFirmId: "Not a law firm",
                       entity: "NEL R. FONTANILLA",
                       serviceAddress: "1536 HEMMINGWAY ROAD, SAN JOSE CA 95132",
                       country: "US",
                       serviceAddressId: "1051425",
                       actions: <IconButton name="cancel" accent />,
                     },
                     {
                       lawFirmId: "Not a law firm",
                       entity: "INTERACTIV CORPORATION",
                       serviceAddress: "1659 N CAPITOL AVE # 225, 1659 N CAPITOL AVE # 225, SAN JOSE, CA 95132",
                       country: "US",
                       website: "",
                       serviceAddressId: "563472",
                       actions: <IconButton name="cancel" accent />,
                     },
                     {
                       lawFirmId: "Not a law firm",
                       entity: "BEAUTYQQ INC",
                       serviceAddress: "2928 LAMBETH COURT, SAN JOSE, CA 95132",
                       country: "US",
                       website: "",
                       serviceAddressId: "584214",
                       actions: <IconButton name="cancel" accent />,
                     },
                     {
                       lawFirmId: <span>44161 <IconButton accent name="open_in_new" /></span>,
                       entity: "PEREZ, YVONNE, CA",
                       serviceAddress: "PEREZ, YVONNE, 2121 LIMEWOOD DR., SAN JOSE, CA 95132",
                       country: "US",
                       website: "",
                       serviceAddressId: "610070",
                       actions: <IconButton name="cancel" accent />,
                     },
                   ]}
          >
          <TableHeader name="lawFirmId">Law Firm ID</TableHeader>
          <TableHeader name="entity">Entity</TableHeader>
          <TableHeader name="serviceAddress">Service Address</TableHeader>
          <TableHeader name="country">Country</TableHeader>
          <TableHeader name="website">Website</TableHeader>
          <TableHeader name="serviceAddressId">Entity ID</TableHeader>
          <TableHeader name="actions">Re-sort</TableHeader>
        </DataTable>
      </section>
    </Content>
    <Footer size="mini" style={{padding: "10px"}}>
      <span>Previous Action: Marking [1427571] PINCHAK, George, L. Tarolli, Sundheim, Covell & Tummino L.L.P. 1300 East Ninth Street, Suite 1700 Cleveland, OH 44114 (US) to Tarolli Sundheim Covell & Tummino, OH</span>
      <Button accent><Icon name="undo" />Undo</Button>
    </Footer>
  </Layout>

export default Root
