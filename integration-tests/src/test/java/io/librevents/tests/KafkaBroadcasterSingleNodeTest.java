package io.librevents.tests;

import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(value = {"mongo", "kafka"})
public class KafkaBroadcasterSingleNodeTest extends BaseKafkaBroadcasterTest {}
