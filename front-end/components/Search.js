// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import {Content, Icon, Spinner, Textfield} from "react-mdl";
import React from "react";
import AddressesList from "./AddressesList";

type
SearchProps = {
  items: Array,
  loading: boolean,
  query: string,
  onSearch: (event: Event) => void,
  onSortServiceAddress: (address: Object) => void
}

const Search = (props: SearchProps): React.Element<Layout> => {
  let content = <p>No data to display. Try alternate the search string.</p>

  if (!props.loading && props.items && props.items.length > 0) {
    content = <AddressesList addresses={props.items} onSortServiceAddress={props.onSortServiceAddress}/>;
  } else if (props.loading) {
    content = <Spinner singleColor/>
  }

  return (
    <div>
      <div>
        <Icon style={{verticalAlign: "sub"}} name="search"/>
        &nbsp;
        <Textfield style={{marginLeft: "5px"}} label="Search by law firm name..." value={props.query}
                   onChange={props.onSearch}/>
      </div>
      <Content className={"center"}>
        {content}
      </Content>
    </div>
  )
}

export default Search