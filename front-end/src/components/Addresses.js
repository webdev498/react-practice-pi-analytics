// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import {Layout, Tab, Tabs} from "react-mdl";
import React from "react";
import SearchContainer from "../containers/SearchContainer";
import AddressesList from "./AddressesList";
import type {Agent} from "../services/Types";

type
AddressesProps = {
  tab: number,
  agents: Array < Agent >,
  serviceAddress: Object,
  queueId: string,
  onTabChange: (tab: number) => void,
  onSortServiceAddress: (queueId: string, serviceAddressId: string, address: Object) => void,
  onUnsortServiceAddress: (serviceAddressId: string) => void
}

const Addresses = (props: AddressesProps): React.Element<Layout> =>
  <div>
    <Tabs activeTab={props.tab} onChange={props.onTabChange} ripple style={{marginTop: "16px"}}>
      <Tab>Suggestions (US)</Tab>
      <Tab>Search</Tab>
    </Tabs>
    <section>
      {props.tab == 0 ? (
        <AddressesList agents={props.agents} queueId={props.queueId} serviceAddress={props.serviceAddress}
                       onSortServiceAddress={props.onSortServiceAddress}
                       onUnsortServiceAddress={props.onUnsortServiceAddress}/>
      ) : (
         <SearchContainer />
       )}
    </section >
  </div >

export default Addresses