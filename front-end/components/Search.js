// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import {Content, Icon, Spinner, Textfield} from "react-mdl";
import React from "react";
import AddressesList from "./AddressesList";

type
SearchProps = {
  agents: Array,
  loading: boolean,
  query: string,
  serviceAddress: Object,
  onSearch: (query: string) => void,
  onSortServiceAddress: (serviceAddressId: string, address: Object) => void,
  onUnsortServiceAddress: (index: number, address: Object) => void
}

const Search = (props: SearchProps): React.Element<Layout> => {
  let content = <p>No data to display. Try alternate the search string.</p>

  if (!props.loading && props.agents && props.agents.length > 0) {
    content =
      <AddressesList agents={props.agents} serviceAddress={props.serviceAddress}
                     onSortServiceAddress={props.onSortServiceAddress}
                     onUnsortServiceAddress={props.onUnsortServiceAddress}/>;
  } else if (props.loading) {
    content = <Spinner singleColor/>
  }

  return (
    <div>
      <div>
        <Icon style={{verticalAlign: "sub"}} name="search"/>
        &nbsp;
        <Textfield style={{marginLeft: "5px"}} label="Search by law firm name..." value={props.query}
                   onChange={(event: Event) => {
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