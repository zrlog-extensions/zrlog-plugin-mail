import {FunctionComponent} from "react";
import EmailIndex from "./components/EmailIndex";
import {EmailInfoResponse} from "./index";

export type AppBaseProps = {
    pluginInfo: EmailInfoResponse;
}

const AppBase: FunctionComponent<AppBaseProps> = ({pluginInfo}) => {
    return <EmailIndex data={pluginInfo}/>;
};

export default AppBase;
