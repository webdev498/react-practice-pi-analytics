// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import {Layout, Tab, Tabs} from "react-mdl";
import React from "react";
import SearchContainer from "../containers/SearchContainer";
import AddressesList from "./AddressesList";
import type {Agent} from "../services/Types";

type
SuggestedAgentsProps = {
  tab: number,
  agents: Array < Agent >,
  serviceAddress: Object,
  onTabChange: (tab: number) => void,
  onSortServiceAddress: (serviceAddressId: string, address: Object) => void,
  onUnsortServiceAddress: (serviceAddressId: string) => void
}

const SuggestedAgents = (props: SuggestedAgentsProps): React.Element<Layout> => {

  const suggestions = props.agents ?
                      <AddressesList agents={props.agents} serviceAddress={props.serviceAddress}
                                     onSortServiceAddress={props.onSortServiceAddress}
                                     onUnsortServiceAddress={props.onUnsortServiceAddress}/> :
                      <h4 className="voffset32" style={{color: "#bbb"}}>No suggestions available for this service
                        address</h4>
  return (
    <div className="addresses-container">
      <Tabs activeTab={props.tab} onChange={props.onTabChange} ripple style={{marginTop: "16px"}}>
        <Tab>Suggestions - {props.serviceAddress.country}</Tab>
        <Tab>Search - {props.serviceAddress.country}</Tab>
      </Tabs>
      <section className={"center"}>
        {props.tab == 0 ? suggestions : (
          <SearchContainer />
        )}
      </section >
    </div >)
}

export default SuggestedAgents