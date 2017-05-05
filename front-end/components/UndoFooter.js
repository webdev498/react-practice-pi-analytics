// Copyright (c) 2017 Practice Insight Pty Ltd. All rights reserved.
//jshint esversion:6
//@flow
import React from "react";
import {Button, Footer, FooterSection, Layout} from "react-mdl";

type
UndoFooterProps = {
  undo: Object,
  disabled: boolean,
  onUndo: (serviceAddressId: string) => void
};

const UndoFooter = (props: UndoFooterProps): React.Element<Layout> => {

  let text = !props.undo ? "No undo action available" : "Assigned ".concat(props.undo.agent.serviceAddresses[0].address,
                                                                           " to ",
                                                                           props.undo.value.serviceAddressToSort.name);

  return (
    <Footer size="mini">
      <FooterSection type="left"><span>{text}</span></FooterSection>
      <FooterSection type="right">
        <Button disabled={props.disabled} onClick={() => {
          props.onUndo(props.undo.value.serviceAddressToSort.serviceAddressId)
        }} raised style={{float: "right"}}>Undo</Button>
      </FooterSection>
    </Footer>
  )
};

export default UndoFooter;