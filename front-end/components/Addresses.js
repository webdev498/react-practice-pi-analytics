// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import {Tab, Tabs} from "react-mdl";
import React from "react";
import SearchContainer from "../containers/SearchContainer";
import AddressesList from "./AddressesList";

type
AddressesProps = {
  tab: number,
  items: Array,
  firmAddress: string,
  onTabChange: (tab: int) => void,
  onBindServiceAddress: (address: Object) => void
}

const Addresses = (props: AddressesProps): React.Element<Layout> =>
  <div>
    <Tabs activeTab={props.tab} onChange={props.onTabChange} ripple style={{marginTop: "16px"}}>
      <Tab>Suggestions (US)</Tab>
      <Tab>Search</Tab>
    </Tabs>
    <section>
      {props.tab == 0 ? (
        <AddressesList addresses={props.items} serviceAddress={props.firmAddress}
                       onBindServiceAddress={props.onBindServiceAddress}/>
      ) : (
         <SearchContainer />
       )}
    </section >
  </div >

export default Addresses