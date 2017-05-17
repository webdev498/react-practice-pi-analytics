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
  let content = <p className="voffset32">Your search did not match any law firms</p>

  if (!props.loading && props.agents && props.agents.length > 0) {
    content =
      <AddressesList agents={props.agents} queueId={props.queueId} serviceAddress={props.serviceAddress}
                     onSortServiceAddress={props.onSortServiceAddress}
                     onUnsortServiceAddress={props.onUnsortServiceAddress}/>;
  } else if (props.loading) {
    content = <Spinner singleColor/>
  }

  return (
    <div>
      <div style={{textAlign: "center"}}>
        <Icon style={{verticalAlign: "sub"}} name="search"/>
        &nbsp;
        <Textfield style={{marginLeft: "5px"}} label="Search by law firm name..." value={props.query}
                   onChange={(event) => {
                     props.onSearch(event.target.value);
                   }}/>
      </div>
      <Content className={"center"}>
        {content}
      </Content>
    </div>
  )
}

export default Search