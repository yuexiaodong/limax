package limax.provider;

import java.util.List;
import java.util.Map;

import limax.net.ManagerConfig;
import limax.net.State;
import limax.util.Enable;

public interface ProviderManagerConfig extends limax.net.Config {

	List<ManagerConfig> getManagerConfigs();

	String getName();

	State getProviderState();

	int getProviderId();

	String getProviderKey();

	Map<Integer, Integer> getProviderProtocolInfos();

	String getTunnelMac();

	byte[] getTunnelKey();

	long getTunnelKeyExpireTime();

	String getViewManagerClassName();

	Enable getAllowUseVariant();

	Enable getAllowUseScript();

	long getSessionTimeout();
}
