package is.erle.captain;

public class SharedHeartbeat {
	/*
	 * MAV_STATE_UNINIT = 0; // Uninitialized system, state is unknown.
	 * MAV_STATE_BOOT = 1; // System is booting up. MAV_STATE_CALIBRATING = 2;
	 * // System is calibrating and not flight-ready. MAV_STATE_STANDBY = 3; //
	 * System is grounded and on standby. It can be launched any time.
	 * MAV_STATE_ACTIVE = 4; // System is active and might be already airborne.
	 * Motors are engaged. MAV_STATE_CRITICAL = 5; // System is in a non-normal
	 * flight mode. It can however still navigate. MAV_STATE_EMERGENCY = 6; //
	 * System is in a non-normal flight mode. It lost control over parts or over
	 * the whole airframe. It is in mayday and going down. MAV_STATE_POWEROFF =
	 * 7; // System just initialized its power-down sequence, will shut down
	 * now. MAV_STATE_ENUM_END = 8;
	 */

	/*
	 * MAV_MODE_FLAG_CUSTOM_MODE_ENABLED = 1; // 0b00000001 Reserved for future
	 * use. MAV_MODE_FLAG_TEST_ENABLED = 2; // 0b00000010 system has a test mode
	 * enabled. This flag is intended for temporary system tests and should not
	 * be used for stable implementations. MAV_MODE_FLAG_AUTO_ENABLED = 4; //
	 * 0b00000100 autonomous mode enabled, system finds its own goal positions.
	 * Guided flag can be set or not, depends on the actual implementation.
	 * MAV_MODE_FLAG_GUIDED_ENABLED = 8; // 0b00001000 guided mode enabled,
	 * system flies MISSIONs / mission items. MAV_MODE_FLAG_STABILIZE_ENABLED =
	 * 16; // 0b00010000 system stabilizes electronically its attitude (and
	 * optionally position). It needs however further control inputs to move
	 * around. MAV_MODE_FLAG_HIL_ENABLED = 32; // 0b00100000 hardware in the
	 * loop simulation. All motors / actuators are blocked, but internal
	 * software is full operational. MAV_MODE_FLAG_MANUAL_INPUT_ENABLED = 64; //
	 * 0b01000000 remote control input is enabled. MAV_MODE_FLAG_SAFETY_ARMED =
	 * 128; // 0b10000000 MAV safety set to armed. Motors are enabled / running
	 * / can start. Ready to fly. MAV_MODE_FLAG_ENUM_END = 129;
	 */

	private volatile String[] heartbeat;
	private volatile boolean isActive;
	private volatile boolean isStandby;
	private volatile boolean isCritical;
	private volatile boolean isEmergency;
	private volatile boolean isModeCustom;
	private volatile boolean isModeTest;
	private volatile boolean isModeGuided;
	private volatile boolean isModeStabilize;
	private volatile boolean isModeManualInput;
	private volatile boolean isModeArmed;
	private volatile boolean isModeAuto;
	private volatile boolean isModeHil;

	public synchronized void set(String[] drone, int mode, int state) {
		heartbeat = new String[drone.length];
		isActive = (state == 4);
		isStandby = (state == 3);
		isCritical = (state == 5);
		isEmergency = (state == 6);
		isModeCustom = ((mode & 0x01) == 0x01);
		isModeTest = ((mode & 0x02) == 0x02);
		isModeAuto = ((mode & 0x04) == 0x04);
		isModeGuided = ((mode & 0x08) == 0x08);
		isModeStabilize = ((mode & 0x10) == 0x10);
		isModeHil = ((mode & 0x20) == 0x20);
		isModeManualInput = ((mode & 0x40) == 0x40);
		isModeArmed = ((mode & 0x80) == 0x80);
	}

	public String[] getHeartbeatString() 
	{
		return heartbeat;
	}

	public boolean getIsModeAuto() 
	{
		return isModeAuto;
	}

	public boolean getIsModeHil() 
	{
		return isModeHil;
	}

	public boolean getIsActive() 
	{
		return isActive;
	}

	public boolean getIsStandby() 
	{
		return isStandby;
	}

	public boolean getIsCritical()
	{
		return isCritical;
	}

	public boolean getIsEmergency() 
	{
		return isEmergency;
	}

	public boolean getIsModeCustom() 
	{
		return isModeCustom;
	}

	public boolean getIModeTest()
	{
		return isModeTest;
	}

	public boolean getIsModeGuided()
	{
		return isModeGuided;
	}

	public boolean getIsModeStabilize()
	{
		return isModeStabilize;
	}

	public boolean getIsModeManualInpu() 
	{
		return isModeManualInput;
	}

	public boolean getIsModeArmed() 
	{
		return isModeArmed;
	}
}