# NOTE: This file is auto generated by OpenAPI Generator 7.0.0-SNAPSHOT (https://openapi-generator.tech).
# Do not edit this file manually.

defmodule OpenapiPetstore.Model.MixedPropertiesAndAdditionalPropertiesClass do
  @moduledoc """
  
  """

  @derive Jason.Encoder
  defstruct [
    :uuid,
    :date_time,
    :map
  ]

  @type t :: %__MODULE__{
    :uuid => String.t | nil,
    :date_time => DateTime.t | nil,
    :map => %{optional(String.t) => OpenapiPetstore.Model.Animal.t} | nil
  }
end

