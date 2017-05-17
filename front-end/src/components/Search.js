// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import {Content, Icon, Layout, Spinner, Textfield} from "react-mdl";
import React from "react";
import AddressesList from "./AddressesList";

type
SearchProps = {
  agents: Array < Object >,
  loading: boolean,
  query: string,
  serviceAddress: Object,
  queueId: string,
  onSearch: (query: string) => void,
  onSortServiceAddress: (queueId: string, serviceAddressId: string, address: Object) => void,
  onUnsortServiceAddress: (serviceAddressId: string) => void
}

const Search = (props: SearchProps): React.Element<Layout> => {
  let content = ""
  if (!props.loading && props.query && props.query.length > 0 && props.agents && props.agents.length > 0) {
    content =
      <AddressesList agents={props.agents} queueId={props.queueId} serviceAddress={props.serviceAddress}
                     onSortServiceAddress={props.onSortServiceAddress}
                     onUnsortServiceAddress={props.onUnsortServiceAddress}/>
  } else if (!props.loading && props.query && props.query.length > 0) {
    content = <h4 className="voffset32" style={{color: "#bbb"}}>No matching law firms</h4>
  } else if (props.loading) {
    content = <Spinner singleColor/>
  }

  return (
    <div>
      <div style={{textAlign: "center", margin: "16px"}}>
        <Icon style={{verticalAlign: "sub"}} name="search"/>
        <Textfield style={{marginLeft: "8px"}} label="Search law firm by name" value={props.query}
                   onChange={(event) => {
                     props.onSearch(event.target.value)
                   }}/>
      </div>
      <Content className={"center"}>
        {content}
      </Content>
    </div>
  )
}

export default Search