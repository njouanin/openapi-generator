# NOTE: This file is auto generated by OpenAPI Generator 7.0.0-SNAPSHOT (https://openapi-generator.tech).
# Do not edit this file manually.

defmodule OpenapiPetstore.Model.FakeBigDecimalMap200Response do
  @moduledoc """
  
  """

  @derive Jason.Encoder
  defstruct [
    :some_id,
    :some_map
  ]

  @type t :: %__MODULE__{
    :some_id => float() | nil,
    :some_map => %{optional(String.t) => float()} | nil
  }
end

